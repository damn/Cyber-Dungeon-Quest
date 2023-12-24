(ns context.entity.modifiers
  (:require context.entity
            cdq.entity))

(defn- effect-modifiers [entity* effect-type]
  (-> entity* :modifiers effect-type))

(extend-type context.entity.Entity
  cdq.entity/EffectModifiers
  (effect-source-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/source))

  (effect-target-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/target)))