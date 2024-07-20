(ns cdq.tx.restore-hp-mana
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [lower-than-max? set-to-max]]
            [cdq.api.context :refer [transact!]]
            [cdq.api.effect :as effect]))

(defn- restore-hp-tx [entity]
  [:tx/assoc entity :entity/hp (set-to-max (:entity/hp @entity))])

(defn- restore-mana-tx [entity]
  [:tx/assoc entity :entity/mana (set-to-max (:entity/mana @entity))])

; => tx/update ?
; [:tx/update entity :entity/mana set-to-max]
; send a function ? not over wire but no problem because its not a low-level txs...

(defcomponent :tx/restore-hp-mana _
  (effect/text [_ _ctx]
    "Restores full hp and mana.")

  (effect/valid-params? [_ {:keys [effect/source]}]
    source)

  (effect/useful? [_ {:keys [effect/source]}]
    (or (lower-than-max? (:entity/mana @source))
        (lower-than-max? (:entity/hp   @source))))

  (transact! [_ {:keys [effect/source]}]
    [(restore-hp-tx source)
     (restore-mana-tx source)]))
