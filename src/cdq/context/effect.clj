(ns cdq.context.effect
  (:require [clojure.string :as str]
            gdl.context
            [cdq.context :refer [transact-all!]]
            [cdq.effect :as effect]))

(defn- deref-source-target [{:keys [effect/source-entity
                                    effect/target-entity]
                             :as effect-ctx}]
  (-> effect-ctx
      ; do not dissoc, there are nested effect/text
      ; for example for projectile -> hit-effect -> damage
      ;(dissoc :effect/source-entity :effect/target-entity)
      (assoc :effect/source (when source-entity @source-entity)
             :effect/target (when target-entity @target-entity))))

(extend-type gdl.context.Context
  cdq.context/EffectInterpreter
  (effect-text [ctx effect]
    (let [ctx (deref-source-target ctx)]
      (->> (keep #(effect/text % ctx) effect)
           (str/join "\n"))))

  (valid-params? [ctx effect]
    (every? #(effect/valid-params? % (deref-source-target ctx))
            effect))

  (effect-useful? [ctx effect]
    (some #(effect/useful? % (deref-source-target ctx)) effect))

  (effect-render-info [ctx effect]
    (let [ctx (deref-source-target ctx)]
      (doseq [component effect]
        (effect/render-info component ctx)))))

(defmethod cdq.context/transact! :tx/effect [[_ effect-ctx effect] ctx]
  (let [ctx (merge ctx effect-ctx)]
    (assert (cdq.context/valid-params? ctx effect)) ; extra line of sight checks here - although we already check valid-params? before always
    (doseq [component effect]
      (->> ctx
           deref-source-target
           (effect/transactions component)
           (transact-all! ctx)))))
