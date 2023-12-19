(ns game.components.state.player-item-on-cursor
  (:require [gdl.context :refer [play-sound! mouse-on-stage-actor?]]
            [game.components.state :as state]
            [game.components.inventory :as inventory]
            [game.entities.item :as item-entity])
  (:import (com.badlogic.gdx Gdx Input$Buttons)))

; TODO ! important ! animation & dont put exactly hiding under player -> try neighbor cells first, simple.
(defn- put-item-on-ground [{:keys [context/player-entity] :as context}]
  {:pre [(:item-on-cursor @player-entity)]}
  (play-sound! context "sounds/bfxr_itemputground.wav")
  (let [{x 0 y 1 :as posi} (:position @player-entity)
        ; [w _] item-body-dimensions
        ; half-size (/ w tile-width 2)
        ; below-posi [x (+ 0.7 y)] ; put here so player sees that item is put on ground (confusing trying to put heal pot on player)
        ; blocked (blocked-location? below-posi half-size half-size :ground)
        ; blocked location checks if other solid bodies ... if put under player would block from player
        ;_ (println "BLOCKED? " (boolean blocked))
        ;position (if-not blocked below-posi posi)
        ]
    (item-entity/create! posi (:item-on-cursor @player-entity) context)))

(defrecord State [entity item]
  state/PlayerState
  (pause-game? [_] true)
  (manual-tick! [_ context delta]
    (when (and (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
               (not (mouse-on-stage-actor? context)))
      (state/send-event! context entity :drop-item)))

  state/State
  (enter [_ _ctx]
    (swap! entity assoc :item-on-cursor item))
  (exit [_ context]
    ; at game.ui.inventory-window/clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (when (:item-on-cursor @entity)
      (put-item-on-ground context)
      (swap! entity dissoc :item-on-cursor)))
  (tick [this delta] this)
  (tick! [_ _ctx _delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
