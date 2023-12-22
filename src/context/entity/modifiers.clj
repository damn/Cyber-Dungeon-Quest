(ns context.entity.modifiers)

(defn effect-modifiers [entity* effect-type]
  (-> entity* :modifiers effect-type))

(defn effect-source-modifiers [entity* effect-type]
  (-> entity* (effect-modifiers effect-type) :effect/source))

(defn effect-target-modifiers [entity* effect-type]
  (-> entity* (effect-modifiers effect-type) :effect/target))
