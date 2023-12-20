(ns game.components.state.npc-dead
  (:require [game.context :refer [audiovisual]]
            [game.components.state :as state]))

(defrecord State [entity]
  state/State
  (enter [_ context]
    (swap! entity assoc :destroyed? true)
    (audiovisual context (:position @entity) :creature/die-effect))
  (exit [_ context])
  (tick [this delta] this)
  (tick! [_ context delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
