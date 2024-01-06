(ns cdq.effect.restore-hp-mana
  (:require [malli.core :as m]
            [x.x :refer [defcomponent]]
            [data.val-max :refer [lower-than-max? set-to-max]]
            [cdq.effect :as effect]))

(def ^:private schema
  (m/schema [:= true]))

(defcomponent :effect/restore-hp-mana _
  (effect/value-schema [_]
    schema)

  (effect/text [_ _ctx]
    "Restores full hp and mana.")

  (effect/valid-params? [_ {:keys [effect/source]}]
    source)

  (effect/useful? [_ {:keys [effect/source]}]
    (or (lower-than-max? (:entity/mana source))
        (lower-than-max? (:entity/hp   source))))

  (effect/transactions [_ {:keys [effect/source]}]
    [(-> source
         (update :entity/hp set-to-max)
         (update :entity/mana set-to-max))]))
