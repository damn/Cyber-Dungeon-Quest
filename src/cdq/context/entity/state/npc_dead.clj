(ns cdq.context.entity.state.npc-dead
  (:require [cdq.context :refer [audiovisual]]
            [cdq.context.entity.state :as state]))

(defrecord NpcDead [entity]
  state/State
  (enter [_ context]
    (swap! entity assoc :entity/destroyed? true)
    (audiovisual context (:entity/position @entity) :creature/die-effect))
  (exit [_ context])
  (tick [_ context])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
