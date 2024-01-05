(ns cdq.state.npc-idle
  (:require [gdl.math.vector :as v]
            [cdq.context :refer [effect-useful? world-grid potential-field-follow-to-enemy skill-usable-state]]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.state.active-skill :refer [deref-source-target-entities]]
            [cdq.world.cell :as cell]))

(defn- effect-context [context entity]
  (let [cell (get (world-grid context)
                  (utils.core/->tile (:entity/position @entity)))
        target (cell/nearest-entity @cell (entity/enemy-faction @entity))]
    {:effect/source-entity entity
     :effect/target-entity target
     :effect/direction (when target
                         (v/direction (:entity/position @entity)
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
(defrecord NpcIdle [entity]
  state/State
  (enter [_ _ctx])
  (exit  [_ _ctx]
    [(assoc @entity :entity/movement-vector nil)])
  (tick [_ context]
    [(assoc @entity :entity/movement-vector (potential-field-follow-to-enemy context entity))
     (let [effect-context (effect-context context entity)]
       (when-let [skill (npc-choose-skill (merge context (deref-source-target-entities effect-context))
                                          @entity)]
         [:tx/event entity :start-action [skill effect-context]]))])

  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
