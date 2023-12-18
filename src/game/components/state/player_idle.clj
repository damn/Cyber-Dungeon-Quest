(ns game.components.state.player-idle
  (:require [gdl.math.vector :as v]
            [gdl.scene2d.stage :as stage]
            [utils.core :refer [safe-get]]
            [data.counter :as counter]
            [game.protocols :as gm]
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
      (gm/play-sound! context "sounds/bfxr_takeit.wav")
      (swap! clicked-entity assoc :destroyed? true)
      (state/send-event! context player-entity :pickup-item item))

     (inventory/try-pickup-item! player-entity item)
     (do
      (gm/play-sound! context "sounds/bfxr_pickup.wav")
      (swap! clicked-entity assoc :destroyed? true))

     :else
     (do
      (gm/play-sound! context "sounds/bfxr_denied.wav")
      (gm/show-msg-to-player! context "Your Inventory is full")))))

(defn- make-effect-params [{:keys [world-mouse-position
                                   context/mouseover-entity]
                            :as context}
                           entity]
  (let [target @mouseover-entity
        target-position (or (and target (:position @target))
                            world-mouse-position)]
    {:source entity
     :target target
     :target-position target-position
     :direction (v/direction (:position @entity)
                             target-position)}))

(defrecord State [entity]
  state/PlayerState
  (pause-game? [_] true)
  (manual-tick! [_
                 {:keys [gui-mouse-position
                         context/mouseover-entity
                         context/properties]
                  :as context}
                 delta]
    (let [stage (:stage (:screens/game context))]  ; TODO hack FIXME
      (if-let [movement-vector (WASD-movement-vector)]
        (state/send-event! context entity :movement-input movement-vector)
        (when (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
          (cond
           (stage/hit stage gui-mouse-position)
           nil

           (clickable/clickable-mouseover-entity? @entity @mouseover-entity)
           (clickable/on-clicked context stage @mouseover-entity)

           :else
           (if-let [skill-id @action-bar/selected-skill-id]
             (let [effect-params (make-effect-params context entity)
                   skill (safe-get properties skill-id)
                   state (skills/usable-state @entity skill effect-params)]
               (if (= state :usable)
                 (state/send-event! context entity :start-action skill effect-params)
                 (gm/show-msg-to-player! context (str "Skill usable state not usable: " state))))
             (gm/show-msg-to-player! context "No selected skill.")))))))

  state/State
  (enter [_ context])
  (exit  [_ context])
  (tick [this delta] this)
  (tick! [_ _context _delta])
  (render-below [_ drawer context entity*])
  (render-above [_ drawer context entity*])
  (render-info  [_ drawer context entity*]))

(comment
 (defn- denied [text]
   ; (gm/play-sound! context "bfxr_denied.wav")
   (gm/show-msg-to-player! context text)))

