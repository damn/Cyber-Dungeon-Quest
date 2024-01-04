(ns cdq.entity.modifiers
  (:require cdq.entity))

(defn- effect-modifiers [entity* effect-type]
  (-> entity* :entity/modifiers effect-type))

(extend-type cdq.entity.Entity
  cdq.entity/EffectModifiers
  (effect-source-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/source))

  (effect-target-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/target)))
