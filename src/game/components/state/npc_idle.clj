(ns game.components.state.npc-idle
  (:require [gdl.math.vector :as v]
            [data.counter :as counter]
            [game.context :refer [effect-useful? world-grid potential-field-follow-to-enemy send-event!]]
            [game.entity :as entity]
            [game.components.skills :as skills]
            [game.world.cell :as cell]))

(defn- make-effect-params [context entity]
  (let [cell (get (world-grid context)
                  (utils.core/->tile (:position @entity)))
        target (cell/nearest-enemy-entity @cell (:faction @entity))]
    {:source entity
     :target target
     :direction (when target
                  (v/direction (:position @entity)
                               (:position @target)))}))

(defn- npc-choose-skill [context entity* effect-params]
  (->> entity*
       :skills
       vals
       (sort-by #(or (:cost %) 0))
       reverse
       (filter #(and (= :usable (skills/usable-state entity*
                                                     %
                                                     effect-params
                                                     context))
                     (effect-useful? (merge context effect-params)
                                     (:effect %))))
       first))

(defrecord State [entity]
  entity/State
  (enter [_ context])

  (exit  [_ context]
    (swap! entity assoc :movement-vector nil))

  (tick [this delta] this)

  (tick! [_ context delta]
    (swap! entity assoc :movement-vector (potential-field-follow-to-enemy context entity))
    (let [effect-params (make-effect-params context entity)]
      (when-let [skill (npc-choose-skill context @entity effect-params)]
        (send-event! context entity :start-action [skill effect-params]))))

  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
