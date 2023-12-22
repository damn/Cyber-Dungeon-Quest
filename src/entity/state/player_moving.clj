(ns entity.state.player-moving
  (:require [game.context :refer [send-event!]]
            [game.entity :as entity]
            [entity.state.wasd-movement :refer [WASD-movement-vector]]))

(defrecord State [entity movement-vector]
  entity/PlayerState
  (pause-game? [_] false)
  (manual-tick! [_ context delta])

  entity/State
  (enter [_ context]
    (swap! entity assoc :movement-vector movement-vector))
  (exit  [_ context]
    (swap! entity dissoc :movement-vector movement-vector))
  (tick [this delta] this)
  (tick! [_ context delta]
    (if-let [movement-vector (WASD-movement-vector context)]
      (swap! entity assoc :movement-vector movement-vector)
      (send-event! context entity :no-movement-input)))
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
