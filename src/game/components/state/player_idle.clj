(ns game.components.state.player-idle
  (:require [gdl.context :refer [play-sound! world-mouse-position gui-mouse-position get-property]]
            [gdl.math.vector :as v]
            [gdl.scene2d.stage :as stage]
            [data.counter :as counter]
            [game.context :as gm]
            [game.effect :as effect]
            [game.components.state :as state]
            [game.components.clickable :as clickable]
            [game.components.inventory :as inventory]
            [game.components.faction :as faction]
            [game.components.skills :as skills]
            [game.components.state.wasd-movement :refer [WASD-movement-vector]]
            [game.ui.action-bar :as action-bar])
  (:import (com.badlogic.gdx Gdx Input$Buttons)
           com.badlogic.gdx.scenes.scene2d.Actor))

(defmethod clickable/on-clicked :item [{:keys [context/player-entity]
                                        :as context}
                                       stage
                                       clicked-entity]
  (let [item (:item @clicked-entity)]
    (cond
     (.isVisible ^Actor (:inventory-window stage))
     (do
      (play-sound! context "sounds/bfxr_takeit.wav")
      (swap! clicked-entity assoc :destroyed? true)
      (state/send-event! context player-entity :pickup-item item))

     (inventory/try-pickup-item! player-entity item)
     (do
      (play-sound! context "sounds/bfxr_pickup.wav")
      (swap! clicked-entity assoc :destroyed? true))

     :else
     (do
      (play-sound! context "sounds/bfxr_denied.wav")
      (gm/show-msg-to-player! context "Your Inventory is full")))))

(defn- make-effect-params [{:keys [context/mouseover-entity] :as context} entity]
  (let [target @mouseover-entity
        target-position (or (and target (:position @target))
                            (world-mouse-position context))]
    {:source entity
     :target target
     :target-position target-position
     :direction (v/direction (:position @entity)
                             target-position)}))

(defrecord State [entity]
  state/PlayerState
  (pause-game? [_] true)
  (manual-tick! [_ {:keys [context/mouseover-entity] :as context} delta]
    (let [stage (:stage (:screens/game context))]  ; TODO hack FIXME
      (if-let [movement-vector (WASD-movement-vector)]
        (state/send-event! context entity :movement-input movement-vector)
        (when (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
          (cond
           (stage/hit stage (gui-mouse-position context))
           nil

           (clickable/clickable-mouseover-entity? @entity @mouseover-entity)
           (clickable/on-clicked context stage @mouseover-entity)

           :else
           (if-let [skill-id @action-bar/selected-skill-id]
             (let [effect-params (make-effect-params context entity)
                   skill (get-property context skill-id)
                   state (skills/usable-state @entity skill effect-params context)]
               (if (= state :usable)
                 (state/send-event! context entity :start-action skill effect-params)
                 (gm/show-msg-to-player! context (str "Skill usable state not usable: " state))))
             (gm/show-msg-to-player! context "No selected skill.")))))))

  state/State
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
   (gm/show-msg-to-player! context text)))

