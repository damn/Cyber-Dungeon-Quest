(ns cdq.context.effect
  (:require [clojure.string :as str]
            gdl.context
            [cdq.context :refer [transact-all!]]
            [cdq.effect :as effect]))

(extend-type gdl.context.Context
  cdq.context/EffectInterpreter
  (effect-text [ctx txs]
    (->> (keep #(effect/text % ctx) txs)
         (str/join "\n")))

  (valid-params? [ctx effect]
    (every? #(effect/valid-params? % ctx) txs))

  (effect-useful? [ctx effect]
    (some #(effect/useful? % ctx) txs))

  (effect-render-info [ctx txs]
    (doseq [tx txs]
      (effect/render-info tx ctx))))

(defmethod cdq.context/transact! :tx/effect [[_ effect-ctx txs] ctx]
  (let [ctx (merge ctx effect-ctx)]
    (assert (effect/valid-params? ctx txs))
    (transact-all! ctx txs)))
