(ns context.entity.state.stunned
  (:require [gdl.context :refer [draw-circle]]
            [game.context :refer [stopped? send-event! ->counter]]
            [context.entity.state :as state]))

(defrecord Stunned [entity counter]
  state/PlayerState
  (pause-game? [_] false)
  (manual-tick! [_ context delta])

  state/State
  (enter [_ _ctx])
  (exit  [_ _ctx])
  (tick! [_ context delta]
    (when (stopped? context counter)
      (send-event! context entity :effect-wears-off)))

  (render-below [_ c {:keys [position]}]
    (draw-circle c position 0.5 [1 1 1 0.6]))

  (render-above [_ c entity*])
  (render-info  [_ c entity*]))

(defn ->CreateWithCounter [entity duration] ; TODO CONTEXT
  (->Stunned entity (->counter context duration)))
