(ns cdq.state.npc-idle
  (:require [gdl.math.vector :as v]
            [cdq.context :refer [effect-useful? world-grid potential-field-follow-to-enemy skill-usable-state]]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.world.cell :as cell]))

(defn- effect-context [context entity*]
  (let [cell (get (world-grid context)
                  (utils.core/->tile (:entity/position entity*)))
        target (cell/nearest-entity @cell (entity/enemy-faction entity*))]
    {:effect/source-entity (entity/reference entity*)
     :effect/target-entity target
     :effect/direction (when target
                         (v/direction (:entity/position entity*)
                                      (:entity/position @target)))}))

(defn- npc-choose-skill [effect-context entity*]
  (->> entity*
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable
                        (skill-usable-state effect-context entity* %))
                     (effect-useful? effect-context (:skill/effect %))))
       first))

; TODO == NpcMoving !!
(defrecord NpcIdle []
  state/State
  (enter [_ entity* _ctx])
  (exit  [_ entity* _ctx]
    [(assoc entity* :entity/movement-vector nil)])
  (tick [_ entity* context]
    [(assoc entity* :entity/movement-vector (potential-field-follow-to-enemy context (entity/reference entity*)))
     (let [effect-context (effect-context context entity*)]
       (when-let [skill (npc-choose-skill (merge context effect-context) entity*)]
         [:tx/event entity* :start-action [skill effect-context]]))])

  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))
