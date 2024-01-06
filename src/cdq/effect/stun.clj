(ns cdq.effect.stun
  (:require [malli.core :as m]
            [x.x :refer [defcomponent]]
            [utils.core :refer [readable-number]]
            [cdq.effect :as effect]))

(def ^:private schema (m/schema [:and number? pos?]))

(defcomponent :effect/stun duration
  (effect/value-schema [_]
    schema)

  (effect/text [_ _ctx]
    (str "Stuns for " (readable-number duration) " seconds"))

  (effect/valid-params? [_ {:keys [effect/source effect/target]}]
    (and target))

  (effect/transactions [_ {:keys [effect/target]}]
    [[:tx/event target :stun duration]]))
