(ns cdq.tx.melee-damage
  (:require [x.x :refer [defcomponent]]
            [cdq.effect :as effect]
            [cdq.context :refer [transact!]]))

(defn- entity*->melee-damage [entity*]
  (let [strength (or (:stats/strength (:entity/stats entity*))
                     0)]
    {:damage/type :physical,
     :damage/min-max [strength strength]}))

(defcomponent :tx/melee-damage _
  (effect/text [_ {:keys [effect/source] :as ctx}]
    (if source
      (effect/text [:tx/damage (entity*->melee-damage @source)] ctx)
      "Damage based on entity stats."))

  (effect/valid-params? [_ {:keys [effect/source effect/target]}]
    (and source target))

  (transact! [_ {:keys [effect/source]}]
    [[:tx/damage (entity*->melee-damage @source)]]))
