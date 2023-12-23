(ns context.entity.state.player-idle
  (:require [gdl.context :refer [play-sound! world-mouse-position mouse-on-stage-actor? button-just-pressed?]]
            [gdl.input.buttons :as buttons]
            [gdl.math.vector :as v]
            [cdq.context :refer [show-msg-to-player! send-event! get-property inventory-window-visible? try-pickup-item! skill-usable-state selected-skill]]
            [context.entity.state :as state]
            [context.entity.state.wasd-movement :refer [WASD-movement-vector]]))

(defmulti ^:private on-clicked (fn [_context entity]
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

     (try-pickup-item! context player-entity item)
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

(defrecord PlayerIdle [entity]
  state/PlayerState
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
         (if-let [skill-id (selected-skill context)]
           (let [effect-context (effect-context context entity)
                 skill (get-property context skill-id)
                 state (skill-usable-state (merge context effect-context) @entity skill)]
             (if (= state :usable)
               (send-event! context entity :start-action [skill effect-context])
               (denied context (str "Skill usable state not usable: " state))))
           (denied context "No selected skill."))))))

  state/State
  (enter [_ context])
  (exit  [_ context])
  (tick! [_ _context _delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
