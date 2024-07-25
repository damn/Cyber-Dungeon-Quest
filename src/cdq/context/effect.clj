(ns cdq.context.effect
  (:require [clojure.string :as str]
            gdl.context
            [cdq.api.context :refer [transact-all! valid-params?]]
            [cdq.api.effect :as effect]))

(extend-type gdl.context.Context
  cdq.api.context/EffectInterpreter
  (effect-text [ctx txs]
    (->> (keep #(effect/text % ctx) txs)
         (str/join "\n")))

  (valid-params? [ctx txs]
    (every? #(effect/valid-params? % ctx) txs))

  (effect-useful? [ctx txs]
    (some #(effect/useful? % ctx) txs))

  (effect-render-info [ctx g txs]
    (doseq [tx txs]
      (effect/render-info tx g ctx))))

(defn- invalid-tx [ctx txs]
  (some #(when (not (effect/valid-params? % ctx)) %) txs))

(defmethod cdq.api.context/transact! :tx/effect [[_ effect-ctx txs] ctx]
  (let [ctx (merge ctx effect-ctx)]
    (assert (valid-params? ctx txs) (pr-str (invalid-tx ctx txs)))
    (transact-all! ctx txs)
    []))
