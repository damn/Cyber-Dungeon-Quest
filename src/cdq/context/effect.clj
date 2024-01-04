(ns cdq.context.effect
  (:require [clojure.string :as str]
            gdl.context
            cdq.context
            [cdq.effect :as effect]))

(extend-type gdl.context.Context
  cdq.context/EffectInterpreter
  (do-effect! [context effect]
    (assert (cdq.context/valid-params? context effect)) ; extra line of sight checks TODO performance issue?
    (doseq [component effect]
      (effect/do! context component)))

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
  (cdq.context/do-effect! (merge ctx effect-ctx)
                          effect))
