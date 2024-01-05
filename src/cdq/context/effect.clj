(ns cdq.context.effect
  (:require [clojure.string :as str]
            gdl.context
            cdq.context
            [cdq.effect :as effect]))

(extend-type gdl.context.Context
  cdq.context/EffectInterpreter
  (effect-text [context effect]
    (->> (keep #(effect/text context %) effect)
         (str/join "\n")))

  (valid-params? [context effect]
    (every? (partial effect/valid-params? context) effect))

  (effect-render-info [context effect]
    (doseq [component effect]
      (effect/render-info context component)))

  (effect-useful? [context effect]
    (some (partial effect/useful? context) effect)))

(defmethod cdq.context/transact! :tx/effect [[_ effect-ctx effect] ctx]
  (let [ctx (merge ctx effect-ctx)]
    (assert (cdq.context/valid-params? ctx effect)) ; extra line of sight checks TODO performance issue?
    (mapcat #(effect/transactions ctx %) effect)))
