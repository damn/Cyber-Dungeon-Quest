; TODO move to game.skills.core
(nsx game.components.skills
  (:require [game.ui.mouseover-entity :refer (saved-mouseover-entity get-mouseover-entity)]
            [game.utils.counter :refer :all]
            [game.utils.msg-to-player :refer (show-msg-to-player)]
            [game.components.hp-mana :refer (enough-mana?)]
            [game.effects.stun :as stun]
            [game.skills.core :as skills]
            [game.maps.potential-field :as potential-field]
            [game.maps.cell-grid :as cell-grid]))

; TODO move to effects/
(defmulti effect-info-render (fn [effect-id effect-params] effect-id))
(defmethod effect-info-render :default [_ _])

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
                         (* (ratio action-counter) 360) ; degree
                         (color/rgb 1 1 1 0.5))
    (image/draw icon [(- x radius) y])))

(def show-skill-icon-on-active true)

(defcomponent :skills skills
  (render-info [_ m position]
    (doseq [{:keys [id image effect]} (vals skills)
            :when (= id (:active-skill? m))]
      (when show-skill-icon-on-active
        (draw-skill-icon image m position)) ; separate component, can deactivate w. component manager tool
      (let [[effect-id effect-value] effect]
        (effect-info-render effect-id
                            ; value is only assoc'ed automatically at effect fns defined in effects/
                            (assoc (effect-params m)
                                   :value
                                   effect-value))))))

(defn- update-cooldown [skill delta]
  (if (:cooling-down? skill)
    (update-finally-merge skill
                          :cooling-down?
                          delta
                          {:cooling-down? false})
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
  (assoc skills id (id skills/skills)))

(modifiers/defmodifier :skill
  {:text skills/text
   :keys [:skills]
   :apply assoc-skill
   :reverse dissoc})

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

(defn- check-enough-mana [entity* skill]
  (let [enough (enough-mana? entity* skill)]
    (when (and (not enough) (:is-player entity*))
      (denied "Not enough mana."))
    enough))

(defn- check-valid-params [entity* skill]
  (let [valid-params (effects/valid-params? (effect-params entity*)
                                            (:effect skill))]
    (when (and (not valid-params) (:is-player entity*))
      (denied "Invalid skill params."))
    ; => need a system for each param 'key' or whatever a separate message etc. ... !
    valid-params))

; TODO no need to check twice for creatures
; and for player do it seperately somehow
; where each failure will be notified (cooldown, not enough mana, effect invalid params error)
(defn- is-usable? [skill entity]
  (and (check-cooldown     @entity skill)
       (check-enough-mana  @entity skill)
       (check-valid-params @entity skill)))
; -> return either true or
; error with reason: cooldown,not-enough-mana,invalid-params: list of params invalid
; => no need to manually call the 3 fns from player-controls

; TODO move to effects/
(defmulti ai-should-use? (fn [[effect-id effect-value] entity] effect-id))
(defmethod ai-should-use? :default [_ entity]
  true)

; TODO no need for multimethod
(defmulti choose-skill (fn [entity] (:choose-skill-type @entity)))

(defmethod choose-skill :player [entity]
  (:chosen-skill-id @entity))

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

; :skillmanager => :skill-modifiers ?
; TODO make generic apply/reverse for numbers w. default-values
; no need to write :apply :reverse, just use + or - or (partial apply-max +)
; apply-val +
(modifiers/defmodifier :cast-speed
  {:values  [[15 25] [35 45] [50 60]]
   :text    #(str "+" % "% Casting-Speed")
   :keys    [:skillmanager :cast-speed]
   :apply   #(+ (or %1 1) (/ %2 100))
   :reverse #(- %1 (/ %2 100))}) ; TODO dissoc again if value == default value -> into modifier logic

(modifiers/defmodifier :attack-speed
  {:values  [[15 25] [35 45] [50 60]]
   :text    #(str "+" % "% Attack-Speed")
   :keys    [:skillmanager :attack-speed]
   :apply   #(+ (or %1 1) (/ %2 100))
   :reverse #(- %1 (/ %2 100))})

(defn- apply-speed-multipliers [entity* skill delta]
  (let [{:keys [cast-speed attack-speed]} (:skillmanager entity*)
        modified-delta (if (:spell? skill)
                         (* delta (or cast-speed   1))
                         (* delta (or attack-speed 1)))]
    ; can be slowed down too, too much slowdown may make 0
    (max 0 (int modified-delta)))) ; TODO same code @ update-speed / tick-entities!

; TODO move action-counter into :active-skill?
(defn- start! [entity skill]
  (->! entity
       (assoc :active-skill? (:id skill))
       (assoc-in [:skillmanager :action-counter] (make-counter (:action-time skill)))))

(defn- stop! [entity skill]
  (->! entity
       (dissoc :active-skill?)
       (update :skillmanager dissoc :action-counter)
       (assoc-in [:skillmanager :effect-params] nil)
       (assoc-in [:skills (:active-skill? @entity) :cooling-down?] (when (:cooldown skill)
                                                                         (make-counter (:cooldown skill))))))

(defn- check-stop! [entity delta]
  (let [id (:active-skill? @entity)
        skill (-> @entity :skills id)
        delta (apply-speed-multipliers @entity skill delta)
        effect-params (effect-params @entity)
        effect (:effect skill)]
    (if-not (effects/valid-params? effect-params effect)
      (stop! entity skill)
      (when (update-counter! entity delta [:skillmanager :action-counter])
        (stop! entity skill)
        (effects/do-effect! effect-params effect)))))

; action-counter => is a sort of counter w. on-stop? implemented ?
; needs to say is-a counter & implement on-stopped?
; and then it calls stop! entity skill and do-effect! ?

; effects are themself just data / w. parameters / also entity/component ?

#_(defcomponent :spell? _
  (start-action-counter [_] "shoot.wav")
  )

(defn- check-start! [entity]
  (set-effect-params! entity) ; => do @ choose-skill ?!
  (let [skill (when-let [id (choose-skill entity)]
                (id (:skills @entity)))]
    (when skill
      (assert (is-usable? skill entity))
      (when-not (or (nil? (:cost skill))
                    (zero? (:cost skill)))
        ; TODO this is overengineered ... just decrement the val-max ??
        ; -> use effects only where I need the text also!
        ; => call directly the affect-mana! fn at :do!
        (effects/do-effect! {:target entity}
                            [:mana [[:val :inc] (- (:cost skill))]]))
      ; spread out speed of cast-sound over action-time ? wooooooooosh
      ; woosh
      ; -> but doesnt work in paused gamestate (pause sounds??)
      (audio/play (if (:spell? skill) "shoot.wav" "slash.wav"))
      (start! entity skill))))

(defcomponent :skillmanager _ ; remove

  (tick! [_ entity delta]
    (swap! entity update :skills update-cooldowns delta) ; make skillss component
    (if (:active-skill? @entity) ; TODO make this in its own component!
      (check-stop! entity delta) ; => active-skill? component & skills just updates its cooldown!!! :D
      (check-start! entity)))

  (stun! [_ entity]
    (when-let [skill-id (:active-skill? @entity)]
      (stop! entity (skill-id (:skills @entity))))))

(defcomponent :skills _
  (create! [[k v] entity]
    (->! entity
         (assoc :skillmanager {}) ; TODO remove skillmanager ? just 'skills' ? update skills ?
         ; all effect-params / action-counter into :active-skill? ??
         (update k #(zipmap % (map skills/skills %))))))
