(ns cdq.state.stunned
  (:require [gdl.context :refer [draw-circle]]
            [cdq.context :refer [stopped? ->counter]]
            [cdq.state :as state]))

(defrecord Stunned [counter]
  state/PlayerState
  (player-enter [_] [[:tx/cursor :cursors/denied]])
  (pause-game? [_] false)
  (manual-tick [_ _entity* context])
  (clicked-inventory-cell [_ cell entity* ctx])
  (clicked-skillmenu-skill [_ skill entity* ctx])

  state/State
  (enter [_ _entity* _ctx])
  (exit  [_ _entity* _ctx])
  (tick [_ entity* ctx]
    (when (stopped? ctx counter)
      [[:tx/event entity* :effect-wears-off]]))

  (render-below [_ {:keys [entity/position]} ctx]
    (draw-circle ctx position 0.5 [1 1 1 0.6]))

  (render-above [_ entity* ctx])
  (render-info  [_ entity* ctx]))

(defn ->CreateWithCounter [ctx _entity* duration]
  (->Stunned (->counter ctx duration)))
