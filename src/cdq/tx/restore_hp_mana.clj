(ns cdq.tx.restore-hp-mana
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [lower-than-max? set-to-max]]
            [cdq.api.context :refer [transact!]]
            [cdq.api.effect :as effect]))

(defmacro def-set-to-max-effect [stat]
  `(defcomponent ~(keyword "tx" (str (name (namespace stat)) "-" (name stat) "-set-to-max")) ~'_
     (effect/text ~'[_ _ctx]
       ~(str "Sets " (name stat) " to max."))

     (effect/valid-params? ~'[_ {:keys [effect/source]}]
       ~'source)

     (effect/useful? ~'[_ {:keys [effect/source]}]
       (lower-than-max? (~stat @~'source)))

     (transact! ~'[_ {:keys [effect/source]}]
       [[:tx/assoc ~'source ~stat (set-to-max (~stat @~'source))]])))

(def-set-to-max-effect :entity/hp)
(def-set-to-max-effect :entity/mana)
