(ns context.entity.state.player-moving
  (:require [cdq.context :refer [send-event! set-cursor!]]
            [context.entity.state :as state]
            [context.entity.state.wasd-movement :refer [WASD-movement-vector]]))

(defrecord PlayerMoving [entity movement-vector]
  state/PlayerState
  (pause-game? [_] false)
  (manual-tick! [_ context delta])
  (allow-ui-clicks? [_] false)

  state/State
  (enter [_ context]
    (set-cursor! context :cursors/walking)
    (swap! entity assoc :movement-vector movement-vector))
  (exit  [_ context]
    (swap! entity dissoc :movement-vector movement-vector))
  (tick! [_ context delta]
    (if-let [movement-vector (WASD-movement-vector context)]
      (swap! entity assoc :movement-vector movement-vector)
      (send-event! context entity :no-movement-input)))
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
