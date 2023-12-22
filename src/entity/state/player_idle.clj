(ns entity.state.player-idle
  (:require [gdl.context :refer [play-sound! world-mouse-position mouse-on-stage-actor? button-just-pressed?]]
            [gdl.input.buttons :as buttons]
            [gdl.math.vector :as v]
            [data.counter :as counter]
            [game.context :refer [show-msg-to-player! send-event! get-property inventory-window-visible?]]
            [game.entity :as entity]
            [entity.clickable :as clickable]
            [entity.inventory :as inventory]
            [game.faction :as faction]
            [entity.skills :as skills]
            [entity.state.wasd-movement :refer [WASD-movement-vector]]
            [context.ui.action-bar :as action-bar]))

(defmulti on-clicked (fn [_context entity]
                       (:type (:entity/clickable @entity))))

(defmethod on-clicked :clickable/item
  [{:keys [context/player-entity] :as context} clicked-entity]
  (let [item (:item @clicked-entity)]
    (cond
     (inventory-window-visible? context)
     (do
      (play-sound! context "sounds/bfxr_takeit.wav")
      (swap! clicked-entity assoc :destroyed? true)
      (send-event! context player-entity :pickup-item item))

     (inventory/try-pickup-item! context player-entity item)
     (do
      (play-sound! context "sounds/bfxr_pickup.wav")
      (swap! clicked-entity assoc :destroyed? true))

     :else
     (do
      (play-sound! context "sounds/bfxr_denied.wav")
      (show-msg-to-player! context "Your Inventory is full")))))

(def ^:private click-distance-tiles 1.5)

(defn- clickable-mouseover-entity? [player-entity* mouseover-entity]
  (and mouseover-entity
       (:entity/clickable @mouseover-entity)
       (< (v/distance (:position player-entity*)
                      (:position @mouseover-entity))
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

(defrecord State [entity]
  entity/PlayerState
  (pause-game? [_] true)
  (manual-tick! [_ {:keys [context/mouseover-entity] :as context} delta]
    (if-let [movement-vector (WASD-movement-vector context)]
      (send-event! context entity :movement-input movement-vector)
      (when (button-just-pressed? context buttons/left)
        (cond
         (mouse-on-stage-actor? context)
         nil

         (clickable-mouseover-entity? @entity @mouseover-entity)
         (on-clicked context @mouseover-entity)

         :else
         (if-let [skill-id @action-bar/selected-skill-id]
           (let [effect-context (effect-context context entity)
                 skill (get-property context skill-id)
                 state (skills/usable-state (merge context effect-context) @entity skill)]
             (if (= state :usable)
               (send-event! context entity :start-action [skill effect-context])
               (denied context (str "Skill usable state not usable: " state))))
           (denied context "No selected skill."))))))

  entity/State
  (enter [_ context])
  (exit  [_ context])
  (tick [this delta] this)
  (tick! [_ _context _delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
