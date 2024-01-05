(ns cdq.entity.state.player-moving
  (:require [cdq.entity.state :as state]
            [cdq.entity.state.wasd-movement :refer [WASD-movement-vector]]))

(defrecord PlayerMoving [entity movement-vector]
  state/PlayerState
  (player-enter [_]
    [[:tx/cursor :cursors/walking]])

  (pause-game? [_] false)
  (manual-tick [_ context])

  state/State
  (enter [_ _ctx]
    [(assoc @entity :entity/movement-vector movement-vector)])
  (exit [_ _ctx]
    [(dissoc @entity :entity/movement-vector movement-vector)])
  (tick [_ context]
    (if-let [movement-vector (WASD-movement-vector context)]
      [(assoc @entity :entity/movement-vector movement-vector)]
      [[:tx/event entity :no-movement-input]]))
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
