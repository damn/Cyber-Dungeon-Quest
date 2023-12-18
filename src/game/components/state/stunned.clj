(ns game.components.state.stunned
  (:require [gdl.graphics.draw :as draw]
            [gdl.graphics.color :as color]
            [data.counter :as counter]
            [game.components.state :as state]))

(defrecord State [entity counter]
  state/PlayerState
  (pause-game? [_] false)
  (manual-tick! [_ context delta])

  state/State
  (enter [_ _ctx])
  (exit  [_ _ctx])
  (tick [this delta]
    (update this :counter counter/tick delta))
  (tick! [_ context delta]
    (when (counter/stopped? counter)
      (state/send-event! context entity :effect-wears-off)))
  (render-below [_ drawer _ctx {:keys [position]}]
    (draw/circle drawer position 0.5 (color/rgb 1 1 1 0.6)))
  (render-above [_ drawer context entity*])
  (render-info  [_ drawer context entity*]))

(defn ->CreateWithCounter [entity duration]
  (->State entity (counter/create duration)))
