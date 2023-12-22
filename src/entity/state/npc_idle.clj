(ns entity.state.npc-idle
  (:require [gdl.math.vector :as v]
            [data.counter :as counter]
            [game.context :refer [effect-useful? world-grid potential-field-follow-to-enemy send-event!]]
            [game.entity :as entity]
            [entity.skills :as skills]
            [game.world.cell :as cell]))

(defn- effect-context [context entity]
  (let [cell (get (world-grid context)
                  (utils.core/->tile (:position @entity)))
        target (cell/nearest-enemy-entity @cell (:faction @entity))]
    {:effect/source entity
     :effect/target target
     :effect/direction (when target
                         (v/direction (:position @entity)
                                      (:position @target)))}))

(defn- npc-choose-skill [effect-context entity*]
  (->> entity*
       :skills
       vals
       (sort-by #(or (:cost %) 0))
       reverse
       (filter #(and (= :usable
                        (skills/usable-state effect-context entity* %))
                     (effect-useful? effect-context (:effect %))))
       first))

(defrecord State [entity]
  entity/State
  (enter [_ context])

  (exit  [_ context]
    (swap! entity assoc :movement-vector nil))

  (tick [this delta] this)

  (tick! [_ context delta]
    (swap! entity assoc :movement-vector (potential-field-follow-to-enemy context entity))
    (let [effect-context (effect-context context entity)]
      (when-let [skill (npc-choose-skill (merge context effect-context) @entity)]
        (send-event! context entity :start-action [skill effect-context]))))

  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
