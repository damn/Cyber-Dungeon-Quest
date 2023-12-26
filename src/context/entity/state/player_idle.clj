(ns context.entity.state.player-idle
  (:require [gdl.context :refer [play-sound! world-mouse-position mouse-on-stage-actor? button-just-pressed?]]
            [gdl.input.buttons :as buttons]
            [gdl.scene2d.actor :refer [visible? toggle-visible!]]
            [gdl.math.vector :as v]
            [context.entity.state :as state]
            [context.entity.state.wasd-movement :refer [WASD-movement-vector]]
            [cdq.context :refer [show-msg-to-player! send-event! get-property inventory-window try-pickup-item! skill-usable-state selected-skill set-cursor!]]))

(defmulti ^:private on-clicked
  (fn [_context entity]
    (:type (:entity/clickable @entity))))

(defmethod on-clicked :clickable/item
  [{:keys [context/player-entity] :as context} clicked-entity]
  (let [item (:item @clicked-entity)]
    (cond
     (visible? (inventory-window context))
     (do
      (play-sound! context "sounds/bfxr_takeit.wav")
      (swap! clicked-entity assoc :destroyed? true)
      (send-event! context player-entity :pickup-item item))

     (try-pickup-item! context player-entity item)
     (do
      (play-sound! context "sounds/bfxr_pickup.wav")
      (swap! clicked-entity assoc :destroyed? true))

     :else
     (do
      (play-sound! context "sounds/bfxr_denied.wav")
      (show-msg-to-player! context "Your Inventory is full")))))

(defmethod on-clicked :clickable/player
  [ctx _clicked-entity]
  (toggle-visible! (inventory-window ctx)))

(def ^:private click-distance-tiles 1.5)

(defn- clickable-mouseover-entity? [player-entity* mouseover-entity*]
  (and (:entity/clickable mouseover-entity*)
       (< (v/distance (:position player-entity*)
                      (:position mouseover-entity*))
          click-distance-tiles)))

(defn- effect-context [{:keys [context/mouseover-entity] :as context} entity]
  (let [target @mouseover-entity
        target-position (or (and target (:position @target))
                            (world-mouse-position context))]
    {:effect/source entity
     :effect/target target
     :effect/target-position target-position
     :effect/direction (v/direction (:position @entity) target-position)}))

(defn- denied [context text]
  (play-sound! context "sounds/bfxr_denied.wav")
  (show-msg-to-player! context text))

(comment
 (.getName (.getParent (mouse-on-stage-actor? @gdl.app/current-context)))
 ; => id
 ; [:cloak [0 0]]
 ; .getName ?
 ; inventory cell = textureregion & parent = the stack

 (.getParent (mouse-on-stage-actor? @gdl.app/current-context))
 )

(defn- ->interaction-state [{:keys [context/mouseover-entity] :as context} entity]
  (cond
   (mouse-on-stage-actor? context)
   ; TODO depending on what ! (window drag, button?)
   ; INVENTORY CELLS ! -> can pickup
   [:cursors/default
    (fn [] nil)]

   (and @mouseover-entity
        (clickable-mouseover-entity? @entity @@mouseover-entity))
   [(case (:type (:entity/clickable @@mouseover-entity))
      :clickable/item :cursors/hand
      :clickable/player :cursors/bag)
    (fn []
      (on-clicked context @mouseover-entity))]

   :else
   (if-let [skill-id (selected-skill context)]
     (let [effect-context (effect-context context entity)
           skill (get-property context skill-id)
           state (skill-usable-state (merge context effect-context) @entity skill)]
       (if (= state :usable)
         (do
          ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
          ; different color ?
          ; => e.g. meditation no TARGET .. etc.
          [:cursors/use-skill
           (fn []
             (send-event! context entity :start-action [skill effect-context])
             )
           ]
          )
         (do
          ; TODO cursor as of usable state
          ; cooldown -> sanduhr kleine
          ; not-enough-mana x mit kreis?
          ; invalid-params -> depends on params ...
          [:cursors/skill-not-usable
           (fn []
             (denied context (str "Skill usable state not usable: " state))
             )
           ]
          )))
     [:cursors/no-skill-selected
      (fn []
        (denied context "No selected skill."))
      ])))

(defrecord PlayerIdle [entity]
  state/PlayerState
  (pause-game? [_] true)

  (manual-tick! [_ context _delta]
    (if-let [movement-vector (WASD-movement-vector context)]
      (send-event! context entity :movement-input movement-vector)
      (let [[cursor on-click] (->interaction-state context entity)]
        (set-cursor! context cursor)
        (when (button-just-pressed? context buttons/left)
          (on-click)))))

  (allow-ui-clicks? [_] true)

  state/State
  (enter [_ context])
  (exit  [_ context])
  (tick! [_ _context _delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
