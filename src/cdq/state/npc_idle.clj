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
    {:effect/source-entity (:entity/id entity*)
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
  (exit  [_ {:keys [entity/id]} _ctx]
    [[:tx/assoc id :entity/movement-vector nil]])
  (tick [_ {:keys [entity/id] :as entity*} context]
    [[:tx/assoc id :entity/movement-vector (potential-field-follow-to-enemy context id)]
     (let [effect-context (effect-context context entity*)]
       (when-let [skill (npc-choose-skill (merge context effect-context) entity*)]
         [:tx/event id :start-action [skill effect-context]]))])

  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))
