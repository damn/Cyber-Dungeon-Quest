(ns game.components.state.player-idle
  (:require [gdl.context :refer [play-sound! world-mouse-position mouse-on-stage-actor? button-just-pressed?]]
            [gdl.input.buttons :as buttons]
            [gdl.math.vector :as v]
            [gdl.scene2d.ui :refer [find-actor-with-id]]
            [data.counter :as counter]
            [game.context :refer [show-msg-to-player! send-event! get-property]]
            [game.entity :as entity]
            [game.components.clickable :as clickable]
            [game.components.inventory :as inventory]
            [game.faction :as faction]
            [game.components.skills :as skills]
            [game.components.state.wasd-movement :refer [WASD-movement-vector]]
            [game.ui.action-bar :as action-bar])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defmethod clickable/on-clicked :item [{:keys [context/player-entity]
                                        :as context}
                                       stage
                                       clicked-entity]
  (let [item (:item @clicked-entity)
        inventory-window (find-actor-with-id (:windows stage) :inventory-window)]
    (cond
     (.isVisible ^Actor inventory-window)
     (do
      (play-sound! context "sounds/bfxr_takeit.wav")
      (swap! clicked-entity assoc :destroyed? true)
      (send-event! context player-entity :pickup-item item))

     (inventory/try-pickup-item! player-entity item)
     (do
      (play-sound! context "sounds/bfxr_pickup.wav")
      (swap! clicked-entity assoc :destroyed? true))

     :else
     (do
      (play-sound! context "sounds/bfxr_denied.wav")
      (show-msg-to-player! context "Your Inventory is full")))))

(defn- effect-context [{:keys [context/mouseover-entity] :as context} entity]
  (let [target @mouseover-entity
        target-position (or (and target (:position @target))
                            (world-mouse-position context))]
    {:effect/source entity
     :effect/target target
     :effect/target-position target-position
     :effect/direction (v/direction (:position @entity) target-position)}))

(defrecord State [entity]
  entity/PlayerState
  (pause-game? [_] true)
  (manual-tick! [_ {:keys [context/mouseover-entity] :as context} delta]
    (let [stage (:stage (:screens/game context))]  ; TODO hack FIXME
      (if-let [movement-vector (WASD-movement-vector context)]
        (send-event! context entity :movement-input movement-vector)
        (when (button-just-pressed? context buttons/left)
          (cond
           (mouse-on-stage-actor? context)
           nil

           (clickable/clickable-mouseover-entity? @entity @mouseover-entity)
           (clickable/on-clicked context stage @mouseover-entity)

           :else
           (if-let [skill-id @action-bar/selected-skill-id]
             (let [effect-context (effect-context context entity)
                   skill (get-property context skill-id)
                   state (skills/usable-state (merge context effect-context) @entity skill)]
               (if (= state :usable)
                 (send-event! context entity :start-action [skill effect-context])
                 (show-msg-to-player! context (str "Skill usable state not usable: " state))))
             (show-msg-to-player! context "No selected skill.")))))))

  entity/State
  (enter [_ context])
  (exit  [_ context])
  (tick [this delta] this)
  (tick! [_ _context _delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))

(comment
 (defn- denied [text]
   ; (play-sound! context "bfxr_denied.wav")
   (show-msg-to-player! context text)))

