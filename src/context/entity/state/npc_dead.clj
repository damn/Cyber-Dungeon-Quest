(ns context.entity.state.npc-dead
  (:require [cdq.context :refer [audiovisual]]
            [context.entity.state :as state]))

(defrecord NpcDead [entity]
  state/State
  (enter [_ context]
    (swap! entity assoc :destroyed? true)
    (audiovisual context (:position @entity) :creature/die-effect))
  (exit [_ context])
  (tick! [_ context])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
