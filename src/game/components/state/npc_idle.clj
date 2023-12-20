(ns game.components.state.npc-idle
  (:require [gdl.math.vector :as v]
            [data.counter :as counter]
            [game.context :refer [potential-field-follow-to-enemy send-event! get-cell]]
            [game.effect :as effect]
            [game.components.faction :as faction]
            [game.components.skills :as skills]
            [game.components.state :as state]))

(defn- nearest-enemy-entity [context {:keys [faction position]}]
  (-> (get-cell context position)
      deref
      ((faction/enemy faction))
      :entity))

(defn- make-effect-params [context entity]
  (let [target (nearest-enemy-entity context @entity)]
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
                     (effect/ai-should-use? (:effect %) effect-params context entity*)))
       first))

(defrecord State [entity]
  state/State
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
