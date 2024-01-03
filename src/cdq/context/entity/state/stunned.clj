(ns cdq.context.entity.state.stunned
  (:require [gdl.context :refer [draw-circle]]
            [cdq.context :refer [stopped? send-event! ->counter set-cursor!]]
            [cdq.context.entity.state :as state]))

(defrecord Stunned [entity counter]
  state/PlayerState
  (player-enter [_ ctx]
    (set-cursor! ctx :cursors/denied))

  (pause-game? [_] false)
  (manual-tick! [_ context])

  state/State
  (enter [_ _ctx])
  (exit  [_ _ctx])
  (tick! [_ context]
    (when (stopped? context counter)
      (send-event! context entity :effect-wears-off)))

  (render-below [_ c {:keys [entity/position]}]
    (draw-circle c position 0.5 [1 1 1 0.6]))

  (render-above [_ c entity*])
  (render-info  [_ c entity*]))

(defn ->CreateWithCounter [context entity duration]
  (->Stunned entity (->counter context duration)))
