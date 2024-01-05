(ns cdq.entity.state.stunned
  (:require [gdl.context :refer [draw-circle]]
            [cdq.context :refer [stopped? ->counter]]
            [cdq.entity.state :as state]))

(defrecord Stunned [entity counter]
  state/PlayerState
  (player-enter [_]
    [[:tx/cursor :cursors/denied]])

  (pause-game? [_] false)
  (manual-tick [_ context])

  state/State
  (enter [_ _ctx])
  (exit  [_ _ctx])
  (tick [_ context]
    (when (stopped? context counter)
      [[:tx/event entity :effect-wears-off]]))

  (render-below [_ c {:keys [entity/position]}]
    (draw-circle c position 0.5 [1 1 1 0.6]))

  (render-above [_ c entity*])
  (render-info  [_ c entity*]))

(defn ->CreateWithCounter [context entity duration]
  (->Stunned entity (->counter context duration)))
