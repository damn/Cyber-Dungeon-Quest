(ns context.entity.state.npc-idle
  (:require [gdl.math.vector :as v]
            [context.entity.state :as state]
            [cdq.context :refer [effect-useful? world-grid potential-field-follow-to-enemy send-event! skill-usable-state]]
            [cdq.entity :as entity]
            [cdq.world.cell :as cell]))

(defn- effect-context [context entity]
  (let [cell (get (world-grid context)
                  (utils.core/->tile (:entity/position @entity)))
        target (cell/nearest-entity @cell (entity/enemy-faction @entity))]
    {:effect/source entity
     :effect/target target
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
  (enter [_ context])

  (exit  [_ context]
    (swap! entity assoc :entity/movement-vector nil))

  (tick! [_ context]
    (swap! entity assoc :entity/movement-vector (potential-field-follow-to-enemy context entity))
    (let [effect-context (effect-context context entity)]
      (when-let [skill (npc-choose-skill (merge context effect-context) @entity)]
        (send-event! context entity :start-action [skill effect-context]))))

  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
