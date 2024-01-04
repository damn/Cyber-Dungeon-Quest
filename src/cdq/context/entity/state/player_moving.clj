(ns cdq.context.entity.state.player-moving
  (:require [cdq.context :refer [set-cursor!]]
            [cdq.context.entity.state :as state]
            [cdq.context.entity.state.wasd-movement :refer [WASD-movement-vector]]))

(defrecord PlayerMoving [entity movement-vector]
  state/PlayerState
  (player-enter [_ ctx]
    (set-cursor! ctx :cursors/walking))

  (pause-game? [_] false)
  (manual-tick! [_ context])

  state/State
  (enter [_ context]
    (swap! entity assoc :entity/movement-vector movement-vector))
  (exit  [_ context]
    (swap! entity dissoc :entity/movement-vector movement-vector))
  (tick [_ context]
    (if-let [movement-vector (WASD-movement-vector context)]
      (assoc @entity :entity/movement-vector movement-vector)
      [:ctx/send-event entity :no-movement-input]))
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
