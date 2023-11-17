(ns game.components.skills
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [apply-val]]
            [gdl.audio :as audio]
            [gdl.graphics.image :as image]
            [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.world :as world]
            [gdl.vector :as v]
            [utils.core :refer [assoc-in! ->! mapvals]]
            [game.tick :refer [tick!]]
            [game.render :as render]
            [game.db :as db]
            [game.properties :as properties]
            [game.components.faction :as faction]
            [game.ui.mouseover-entity :refer (saved-mouseover-entity get-mouseover-entity)]
            [game.utils.counter :as counter]
            [game.utils.msg-to-player :refer (show-msg-to-player)]
            [game.effect :as effect]
            [game.effects.stun :as stun]
            [game.maps.cell-grid :as cell-grid]))

(defn- make-effect-params [entity]
  (merge {:source entity}
         (if (:is-player @entity)
           (let [target (or (saved-mouseover-entity)
                            (get-mouseover-entity))
                 target-position (or (and target (:position @target))
                                     (world/mouse-position))]
             {:target target
              :target-position target-position
              :direction (v/direction (:position @entity)
                                      target-position)})
           (let [faction (faction/enemy (:faction @entity))
                 target (-> @entity
                            :position
                            cell-grid/get-cell
                            deref
                            faction
                            :entity)]
             {:target target
              :direction (when target
                           (v/direction (:position @entity)
                                        (:position @target)))}))))

(defn- set-effect-params! [entity]
  (assoc-in! entity [:skillmanager :effect-params] (make-effect-params entity)))

(defn- effect-params [entity*]
  (:effect-params (:skillmanager entity*)))

; TODO draw one frame more in red after effect was done ...
(defn- draw-skill-icon [icon entity* [x y]]
  (let [[width height] (image/world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ width 2)
        y (+ y (:half-height (:body entity*)))
        action-counter (get-in entity* [:skillmanager :action-counter])
        center [x (+ y radius)]]
    (shape-drawer/filled-circle center radius (color/rgb 1 1 1 0.125))
    ; TODO clockwise ?
    (shape-drawer/sector center
                         radius
                         0 ; start-angle
                         (* (counter/ratio action-counter) 360) ; degree
                         (color/rgb 1 1 1 0.5))
    (image/draw icon [(- x radius) y])))

(def show-skill-icon-on-active true)

(defcomponent :skills skills
  (render/info [_ m position]
    (doseq [{:keys [id image effect]} (vals skills)
            :when (= id (:active-skill? m))]
      (when show-skill-icon-on-active
        (draw-skill-icon image m position)) ; separate component, can deactivate w. component manager tool
      (effect/render-info effect (effect-params m)))))

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

; only used @ revive.
(defn reset-cooldowns [skills]
  (mapvals #(assoc % :cooling-down? false)
           skills))

(defn has-skill? [entity* id]
  (contains? (:skills entity*) id))

(defn assoc-skill [skills id]
  {:pre [(not (contains? skills id))]}
  (assoc skills id (properties/get id)))

; TODO move denied (player-centric) code to
; game.player.controls

(defn- denied [text]
  ;(audio/play "bfxr_denied.wav")
  ; TODO play once only -> need to use Music class internally for isPlaying()
  ; annoying when you hold mouse for the skill
  ; only when not hold mouse...
  (show-msg-to-player text))

(defn- check-cooldown [entity* skill]
  (let [cooling-down? (:cooling-down? skill)]
    (when (and cooling-down? (:is-player entity*))
      (denied "Skill is on cooldown."))
    (not cooling-down?)))

(defn- enough-mana? [entity* {:keys [cost] :as skill}]
  (or (nil? cost)
      (zero? cost) ; TODO zero cost means entity doesnt need mana?? no structure!
      (<= cost ((:mana entity*) 0))))

(defn- check-enough-mana [entity* skill]
  (let [enough (enough-mana? entity* skill)]
    (when (and (not enough) (:is-player entity*))
      (denied "Not enough mana."))
    enough))

(defn- check-valid-params [entity* skill]
  (let [valid-params (effect/valid-params? (:effect skill)
                                           (effect-params entity*))]
    (when (and (not valid-params) (:is-player entity*))
      (denied "Invalid skill params."))
    ; => need a system for each param 'key' or whatever a separate message etc. ... !
    valid-params))

; TODO no need to check twice for creatures
; and for player do it seperately somehow
; where each failure will be notified (cooldown, not enough mana, effect invalid params error)
(defn is-usable? [skill entity]
  (and (check-cooldown     @entity skill)
       (check-enough-mana  @entity skill)
       (check-valid-params @entity skill)))
; -> return either true or
; error with reason: cooldown,not-enough-mana,invalid-params: list of params invalid
; => no need to manually call the 3 fns from player-controls

; => usable-state = :usable
; or :on-cooldown
; or :not-enough-mana
; or [:invalid-params {params-info}]

; TODO move to effect/
(defmulti ai-should-use? (fn [[effect-type effect-value] entity] effect-type))
(defmethod ai-should-use? :default [_ entity]
  true)

(defmulti choose-skill (fn [entity] (:choose-skill-type @entity)))

(defmethod choose-skill :npc [entity]
  (->> @entity
       :skills
       vals
       (sort-by #(or (:cost %) 0))
       reverse
       (filter #(and (is-usable? % entity)
                     (ai-should-use? (:effect %) entity))) ; TODO pass (effect-params @entity)
       first
       :id))

(defn- apply-speed-multipliers [entity* skill delta]
  (let [{:keys [cast-speed attack-speed]} (:skillmanager entity*)
        modified-delta (if (:spell? skill)
                         (* delta (or cast-speed   1))
                         (* delta (or attack-speed 1)))]
    ; can be slowed down too, too much slowdown may make 0
    (max 0 (int modified-delta)))) ; TODO same code @ update-speed / tick-entities!

; TODO move action-counter & effect-params into :active-skill? component
; -> remove skillmanager
; => but grep skillmanager, there is cast-speed/attack-speed (:modifiers component ?)
; and :blocks ( blocks skills and active-skill both ? )
; or move everything into 'skills' ?
; just :active-skill without question mark?
(defn- start! [entity skill]
  (audio/play (if (:spell? skill) "shoot.wav" "slash.wav"))
  (when-not (or (nil? (:cost skill))
                (zero? (:cost skill)))
    (swap! entity update :mana apply-val #(- % (:cost skill))))
  (->! entity
       (assoc :active-skill? (:id skill))
       (assoc-in [:skillmanager :action-counter] (counter/create (:action-time skill)))))

(defn- stop! [entity skill]
  (->! entity
       (dissoc :active-skill?)
       (update :skillmanager dissoc :action-counter)
       (assoc-in [:skillmanager :effect-params] nil)
       (assoc-in [:skills (:active-skill? @entity) :cooling-down?] (when (:cooldown skill)
                                                                     (counter/create (:cooldown skill))))))

(defn- check-stop! [entity delta]
  (let [id (:active-skill? @entity)
        skill (-> @entity :skills id)
        delta (apply-speed-multipliers @entity skill delta)
        effect-params (effect-params @entity)
        effect (:effect skill)]
    (if-not (effect/valid-params? effect effect-params)
      (stop! entity skill)
      (do
       (swap! entity update-in [:skillmanager :action-counter] counter/tick delta)
       (when (counter/stopped? (get-in @entity [:skillmanager :action-counter]))
         (stop! entity skill)
         (effect/do! effect effect-params))))))

(defn- check-start! [entity]
  (set-effect-params! entity)
  (let [skill (when-let [id (choose-skill entity)]
                (id (:skills @entity)))]
    (when skill
      (assert (is-usable? skill entity))
      (start! entity skill))))

(defcomponent :skillmanager _
  (tick! [_ entity delta]
    (swap! entity update :skills update-cooldowns delta)
    (if (:active-skill? @entity)
      (check-stop! entity delta)
      (check-start! entity)))
  (stun/stun! [_ entity]
    (when-let [skill-id (:active-skill? @entity)]
      (stop! entity (skill-id (:skills @entity))))))

(defcomponent :skills _
  (db/create! [[k v] entity]
    (->! entity
         (assoc :skillmanager {})
         (update k #(zipmap % (map properties/get %))))))
