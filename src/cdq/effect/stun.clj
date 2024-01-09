(ns cdq.effect.stun
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [readable-number]]
            [cdq.effect :as effect]))

(defcomponent :effect/stun duration
  (effect/text [_ _ctx]
    (str "Stuns for " (readable-number duration) " seconds"))

  (effect/valid-params? [_ {:keys [effect/source effect/target]}]
    (and target))

  (effect/transactions [_ {:keys [effect/target]}]
    [[:tx/event (:entity/id target) :stun duration]]))
