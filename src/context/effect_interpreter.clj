(ns context.effect-interpreter
  (:require gdl.context
            game.context))

; TODO make all spells effects with ns key
; TODO make them also [ ]  => a seq and adjust valid params etc.

; TODO grep source/target
; TODO effects/damage ? namespaced ? maybe hit-effects/ bla-effects/
; TODO everywhere not value but type value passed

(defn- by-type [_context [type _value]]
  type)

(defmulti do!           by-type)
(defmulti text          by-type)
(defmulti valid-params? by-type)
(defmulti render-info   by-type)
(defmethod render-info :default [_ _])
(defmulti useful?       by-type)
(defmethod useful? :default [_ _] true)

(extend-type gdl.context.Context
  game.context.EffectInterpreter
  (do-effect! [context effect]
    ; (assert (valid-params? context effect)) ; TODO checking line of sight, etc again here , should already be checked
    (doseq [component effect]
      (do! context component)))

  (effect-text [context effect]
    (->> (for [component effect]
           (text context component))
         (str/join "\n")))

  (valid-params? [context effect]
    ; TODO only used @ skill, 1-part-effects only yet.
    (valid-params? context effect))

  (effect-render-info [context effect]
    ; TODO only used @ skill, 1-part-effects only yet.
    (render-info context effect))

  (effect-useful? [_ effect]
    ; TODO only used @ skill, 1-part-effects only yet.
    (useful? context effect)))
