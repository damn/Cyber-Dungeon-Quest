(ns context.entity.modifiers
  (:require context.ecs
            cdq.entity))

(defn- effect-modifiers [entity* effect-type]
  (-> entity* :entity/modifiers effect-type))

(extend-type context.ecs.Entity
  cdq.entity/EffectModifiers
  (effect-source-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/source))

  (effect-target-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/target)))
