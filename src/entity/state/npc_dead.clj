(ns entity.state.npc-dead
  (:require [game.context :refer [audiovisual]]
            [game.entity :as entity]))

(defrecord State [entity]
  entity/State
  (enter [_ context]
    (swap! entity assoc :destroyed? true)
    (audiovisual context (:position @entity) :creature/die-effect))
  (exit [_ context])
  (tick [this delta] this)
  (tick! [_ context delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
