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
    [[:tx/dissoc id :entity/movement-vector]])

  (tick [_ {:keys [entity/id]} ctx]
    (when (stopped? ctx counter)
      [[:tx/event id :timer-finished]]))

  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))

(defn ->npc-moving [ctx {:keys [entity/reaction-time]} movement-direction]
  (->NpcMoving movement-direction (->counter ctx reaction-time)))
