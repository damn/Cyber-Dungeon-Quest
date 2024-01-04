(ns cdq.entity.state.npc
  (:require [reduce-fsm :as fsm]
            (cdq.entity.state [active-skill :as active-skill]
                              [npc-dead :as npc-dead]
                              [npc-idle :as npc-idle]
                              [npc-sleeping :as npc-sleeping]
                              [stunned :as stunned])))

(fsm/defsm-inc ^:private npc-fsm
  [[:sleeping
    :kill -> :dead
    :stun -> :stunned
    :alert -> :idle]
   [:idle
    :kill -> :dead
    :stun -> :stunned
    :start-action -> :active-skill]
   [:active-skill
    :kill -> :dead
    :stun -> :stunned
    :action-done -> :idle]
   [:stunned
    :kill -> :dead
    :effect-wears-off -> :idle]
   [:dead]])

(def ^:private npc-state-constructors
  {:sleeping     (fn [_ctx e] (npc-sleeping/->NpcSleeping e))
   :idle         (fn [_ctx e] (npc-idle/->NpcIdle e))
   :active-skill active-skill/->CreateWithCounter
   :stunned      stunned/->CreateWithCounter
   :dead         (fn [_ctx e] (npc-dead/->NpcDead e))})

(defn ->state [initial-state]
  {:initial-state initial-state
   :fsm npc-fsm
   :state-obj-constructors npc-state-constructors})
