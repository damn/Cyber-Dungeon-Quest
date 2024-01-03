(ns cdq.context.entity.modifiers
  (:require cdq.context.ecs
            cdq.entity))

(defn- effect-modifiers [entity* effect-type]
  (-> entity* :entity/modifiers effect-type))

(extend-type cdq.context.ecs.Entity
  cdq.entity/EffectModifiers
  (effect-source-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/source))

  (effect-target-modifiers [entity* effect-type]
    (-> entity* (effect-modifiers effect-type) :effect/target)))
