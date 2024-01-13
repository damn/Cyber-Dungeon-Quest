(ns cdq.state.player-moving
  (:require [cdq.state :as state]
            [cdq.state.wasd-movement :refer [WASD-movement-vector]]))

(defrecord PlayerMoving [movement-vector]
  state/PlayerState
  (player-enter [_] [[:tx/cursor :cursors/walking]])
  (pause-game? [_] false)
  (manual-tick [_ entity* context])
  (clicked-inventory-cell [_ entity* cell])
  (clicked-skillmenu-skill [_ entity* skill])

  state/State
  (enter [_ {:keys [entity/id]} _ctx]
    [[:tx/assoc id :entity/movement-vector movement-vector]])
  (exit [_ {:keys [entity/id]} _ctx]
    [[:tx/dissoc id :entity/movement-vector]])
  (tick [_ {:keys [entity/id]} context]
    (if-let [movement-vector (WASD-movement-vector context)]
      [[:tx/assoc id :entity/movement-vector movement-vector]]
      [[:tx/event id :no-movement-input]]))
  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))
