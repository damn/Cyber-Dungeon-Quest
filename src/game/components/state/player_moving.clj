(ns game.components.state.player-moving
  (:require [gdl.scene2d.stage :as stage]
            [utils.core :refer [safe-get]]
            [data.counter :as counter]
            [game.protocols :as gm]
            [game.effect :as effect]
            [game.components.state :as state]
            [game.components.clickable :as clickable]
            [game.components.inventory :as inventory]
            [game.components.faction :as faction]
            [game.components.state.wasd-movement :refer [WASD-movement-vector]])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defrecord State [entity movement-vector]
  state/PlayerState
  (pause-game? [_] false)
  (manual-tick! [_ context delta])

  state/State
  (enter [_ context]
    (swap! entity assoc :movement-vector movement-vector))
  (exit  [_ context]
    (swap! entity dissoc :movement-vector movement-vector))
  (tick [this delta] this)
  (tick! [_ context delta]
    (if-let [movement-vector (WASD-movement-vector)]
      (swap! entity assoc :movement-vector movement-vector)
      (state/send-event! context entity :no-movement-input)))
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
