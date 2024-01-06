(ns cdq.state.player-moving
  (:require [cdq.state :as state]
            [cdq.state.wasd-movement :refer [WASD-movement-vector]]))

(defrecord PlayerMoving [movement-vector]
  state/PlayerState
  (player-enter [_] [[:tx/cursor :cursors/walking]])
  (pause-game? [_] false)
  (manual-tick [_ entity* context])
  (clicked-inventory-cell [_ cell entity* ctx])
  (clicked-skillmenu-skill [_ skill entity* ctx])

  state/State
  (enter [_ entity* _ctx]
    [(assoc entity* :entity/movement-vector movement-vector)])
  (exit [_ entity* _ctx]
    [(dissoc entity* :entity/movement-vector movement-vector)])
  (tick [_ entity* context]
    (if-let [movement-vector (WASD-movement-vector context)]
      [(assoc entity* :entity/movement-vector movement-vector)]
      [[:tx/event entity* :no-movement-input]]))
  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))
