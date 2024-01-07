(ns cdq.state.npc-moving
  (:require [cdq.context :refer [stopped? ->counter]]
            [cdq.state :as state]))

; npc moving is basically a performance optimization so npcs do not have to check
; pathfinding/usable skills every frame
(defrecord NpcMoving [movement-vector counter]
  state/State
  (enter [_ {:keys [entity/id]} _ctx]
    [[:tx/assoc id :entity/movement-vector movement-vector]])

  (exit [_ {:keys [entity/id]} _ctx]
    [[:tx/dissoc id :entity/movement-vector movement-vector]])

  (tick [_ {:keys [entity/id]} ctx]
    (when (stopped? ctx counter)
      [[:tx/event id :timer-finished]]))

  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))

; TODO 0.5 = moving-state-time / reaction-time
; (* 0.2 (rand))
(defn ->npc-moving [ctx _entity* movement-direction]
  (->NpcMoving movement-direction (->counter ctx 0.2)))
