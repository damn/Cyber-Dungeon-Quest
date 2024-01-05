(ns cdq.entity.state.npc-dead
  (:require [cdq.entity.state :as state]))

(defrecord NpcDead [entity]
  state/State
  (enter [_ _ctx]
    [(assoc @entity :entity/destroyed? true)
     [:tx/audiovisual (:entity/position @entity) :creature/die-effect]])
  (exit [_ _ctx])
  (tick [_ _ctx])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
