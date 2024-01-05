(ns cdq.state.npc-dead
  (:require [cdq.state :as state]))

(defrecord NpcDead []
  state/State
  (enter [_ entity* _ctx]
    [(assoc entity* :entity/destroyed? true)
     [:tx/audiovisual (:entity/position entity*) :creature/die-effect]])
  (exit [_ entity* _ctx])
  (tick [_ entity* _ctx])
  (render-below [_ entity* ctx])
  (render-above [_ entity* ctx])
  (render-info  [_ entity* ctx]))
