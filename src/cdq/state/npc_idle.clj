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
    {:effect/source (:entity/id entity*)
     :effect/target target
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

(defrecord NpcIdle []
  state/State
  (enter [_ entity* _ctx])
  (exit  [_ entity* _ctx])
  (tick [_ {:keys [entity/id] :as entity*} context]
    (let [effect-context (effect-context context entity*)]
      (if-let [skill (npc-choose-skill (merge context effect-context) entity*)]
        [[:tx/event id :start-action [skill effect-context]]]
        [[:tx/event id :movement-direction (or (potential-field-follow-to-enemy context id)
                                               [0 0])]]))) ; nil param not accepted @ entity.state

  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))
