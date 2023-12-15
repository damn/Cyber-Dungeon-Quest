(ns game.components.skills
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [apply-val]]
            [gdl.graphics.draw :as draw]
            [gdl.math.vector :as v]
            [gdl.graphics.color :as color]
            [utils.core :refer [safe-get mapvals]]
            [game.properties :as properties]
            [game.context :as gm]
            [game.components.position :as position]
            [game.components.faction :as faction]
            [game.ui.mouseover-entity :refer (saved-mouseover-entity get-mouseover-entity)]
            [game.utils.counter :as counter]
            [game.effect :as effect]
            [game.entity :as entity]))

(defn- nearest-enemy-entity [cell-grid entity*]
  (let [enemy-faction (faction/enemy (:faction entity*))]
    (-> @(get cell-grid (position/get-tile entity*))
        enemy-faction
        :entity)))

(defn- make-effect-params [{:keys [world-mouse-position context/world-map]} entity]
  (merge {:source entity}
         (if (:is-player @entity)
           (let [target (or (saved-mouseover-entity)
                            (get-mouseover-entity))
                 target-position (or (and target (:position @target))
                                     world-mouse-position)]
             {:target target
              :target-position target-position
              :direction (v/direction (:position @entity)
                                      target-position)})
           (let [target (nearest-enemy-entity (:cell-grid world-map)
                                              @entity)]
             {:target target
              :direction (when target
                           (v/direction (:position @entity)
                                        (:position @target)))}))))


(defn- effect-params [entity*]
  (:effect-params (:skillmanager entity*)))

(defn- draw-skill-icon [drawer icon entity* [x y]]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ width 2)
        y (+ y (:half-height (:body entity*)))
        action-counter (get-in entity* [:skillmanager :action-counter])
        center [x (+ y radius)]]
    (draw/filled-circle drawer center radius (color/rgb 1 1 1 0.125))
    (draw/sector drawer
                 center
                 radius
                 0 ; start-angle
                 (* (counter/ratio action-counter) 360) ; degree
                 (color/rgb 1 1 1 0.5))
    (draw/image drawer icon [(- x radius) y])))

(def show-skill-icon-on-active true)

(defcomponent :skills skills
  (entity/render-info [_ drawer context {:keys [position active-skill?] :as entity*}]
    (doseq [{:keys [id image effect]} (vals skills)
            :when (= id active-skill?)]
      (when show-skill-icon-on-active
        (draw-skill-icon drawer image entity* position))
      (effect/render-info drawer effect (effect-params entity*)))))

(defn- update-cooldown [skill delta]
  (if (:cooling-down? skill)
    (update skill :cooling-down?
            #(let [counter (counter/tick % delta)]
               (if (counter/stopped? counter)
                 false
                 counter)))
    skill))

(defn- update-cooldowns [skills delta]
  (mapvals #(update-cooldown % delta)
           skills))

(defn has-skill? [entity* id]
  (contains? (:skills entity*) id))

(defn assoc-skill [skills id]
  {:pre [(not (contains? skills id))]}
  (assoc skills id (properties/get id)))

(defn- enough-mana? [entity* {:keys [cost] :as skill}]
  (or (nil? cost)
      (zero? cost)
      (<= cost ((:mana entity*) 0))))

(defn usable-state [entity* skill]
  (cond
   (:cooling-down? skill)
   :cooldown
   (not (enough-mana? entity* skill))
   :not-enough-mana
   (not (effect/valid-params? (:effect skill)
                              (effect-params entity*)))
   :invalid-params
   :else
   :usable))

(defmulti ai-should-use?
  (fn [[effect-type _effect-value] _context _entity*]
    effect-type))

(defmethod ai-should-use? :default [_ _context _entity*]
  true)

(defmulti choose-skill
  (fn [_context entity*]
    (:choose-skill-type entity*)))

(defmethod choose-skill :npc [context entity*]
  (->> entity*
       :skills
       vals
       (sort-by #(or (:cost %) 0))
       reverse
       (filter #(and (= :usable (usable-state entity* %))
                     (ai-should-use? (:effect %) context entity*)))
       first
       :id))

(defn- apply-speed-multipliers [entity* skill delta]
  (let [{:keys [cast-speed attack-speed]} (:modifiers entity*)
        modified-delta (if (:spell? skill)
                         (* delta (or cast-speed   1))
                         (* delta (or attack-speed 1)))]
    (max 0 (int modified-delta))))

(defn- start-skill [entity* skill]
  (let [entity* (if-not (or (nil? (:cost skill))
                            (zero? (:cost skill)))
                  (update entity* :mana apply-val #(- % (:cost skill)))
                  entity*)]
    (-> entity*
        (assoc :active-skill? (:id skill))
        (assoc-in [:skillmanager :action-counter] (counter/create (:action-time skill))))))

(defn- stop-skill [entity* skill]
  (-> entity*
      (dissoc :active-skill?)
      (update :skillmanager dissoc :action-counter)
      (assoc-in [:skillmanager :effect-params] nil)
      (assoc-in [:skills (:active-skill? entity*) :cooling-down?] (when (:cooldown skill)
                                                                    (counter/create (:cooldown skill))))))

(defn- check-stop! [context entity delta]
  (let [id (:active-skill? @entity)
        skill (-> @entity :skills id)
        delta (apply-speed-multipliers @entity skill delta)
        effect-params (effect-params @entity)
        effect (:effect skill)]
    (if-not (effect/valid-params? effect effect-params)
      (swap! entity stop-skill skill)
      (do
       (swap! entity update-in [:skillmanager :action-counter] counter/tick delta)
       (when (counter/stopped? (get-in @entity [:skillmanager :action-counter]))
         (swap! entity stop-skill skill)
         (effect/do! effect effect-params context))))))

(defn- check-start! [context entity]
  (swap! entity assoc-in [:skillmanager :effect-params] (make-effect-params context entity))
  (let [skill (when-let [id (choose-skill context @entity)]
                (id (:skills @entity)))]
    (when skill
      (assert (= :usable (usable-state @entity skill)))
      (gm/play-sound! context (str "sounds/" (if (:spell? skill) "shoot.wav" "slash.wav")))
      (swap! entity start-skill skill))))

(defcomponent :skillmanager _
  (entity/tick! [_ context entity delta]
    (swap! entity update :skills update-cooldowns delta)
    (if (:active-skill? @entity)
      (check-stop! context entity delta)
      (check-start! context entity)))
  (entity/stun! [_ entity]
    (when-let [skill-id (:active-skill? @entity)]
      (swap! entity stop-skill (skill-id (:skills @entity))))))

(defcomponent :skills _
  (entity/create! [[k v] entity {:keys [context/properties]}]
    (swap! entity (fn [e*] (-> e*
                               (assoc :skillmanager {})
                               (update k #(zipmap % (map (fn [skill-id]
                                                           (safe-get properties skill-id))
                                                         %))))))))
