(ns cdq.effect.restore-hp-mana
  (:require [malli.core :as m]
            [data.val-max :refer [lower-than-max? set-to-max]]
            [cdq.effect :as effect]))

(def ^:private schema
  (m/schema [:= true]))

(defmethod effect/value-schema :effect/restore-hp-mana [_]
  schema)

(defmethod effect/useful? :effect/restore-hp-mana
  [{:keys [effect/source]} _effect]
  (or (lower-than-max? (:entity/mana source))
      (lower-than-max? (:entity/hp   source))))

(defmethod effect/text :effect/restore-hp-mana
  [_context _effect]
  "Restores full hp and mana.")

(defmethod effect/valid-params? :effect/restore-hp-mana
  [{:keys [effect/source]} _effect]
  source)

(defmethod effect/transactions :effect/restore-hp-mana
  [{:keys [effect/source] :as context} _effect]
  [(-> source
       (update :entity/hp set-to-max)
       (update :entity/mana set-to-max))])
