(ns game.components.modifiers)

(defn effect-modifiers [entity* effect-type]
  (-> entity* :modifiers effect-type))

(defn effect-source-modifiers [entity* effect-type]
  (-> entity* (effect-modifiers effect-type) :source))

(defn effect-target-modifiers [entity* effect-type]
  (-> entity* (effect-modifiers effect-type) :target))
