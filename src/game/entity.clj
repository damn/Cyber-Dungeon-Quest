(ns game.entity)

(defprotocol State
  (state [_]))

(defprotocol Skills
  (has-skill? [_ skill]))

(defprotocol EffectModifiers
  (effect-source-modifiers [_ effect-type])
  (effect-target-modifiers [_ effect-type]))
