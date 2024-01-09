(ns cdq.effect.restore-hp-mana
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [lower-than-max? set-to-max]]
            [cdq.effect :as effect]))

(defn- restore-hp-tx [{:keys [entity/id entity/hp]}]
  [:tx/assoc id :entity/hp (set-to-max hp)])

(defn- restore-mana-tx [{:keys [entity/id entity/mana]}]
  [:tx/assoc id :entity/mana (set-to-max mana)])

(defcomponent :effect/restore-hp-mana _
  (effect/text [_ _ctx]
    "Restores full hp and mana.")

  (effect/valid-params? [_ {:keys [effect/source]}]
    source)

  (effect/useful? [_ {:keys [effect/source]}]
    (or (lower-than-max? (:entity/mana source))
        (lower-than-max? (:entity/hp   source))))

  (effect/transactions [_ {:keys [effect/source]}]
    [(restore-hp-tx source)
     (restore-mana-tx source)]))
