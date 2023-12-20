(ns context.effect-interpreter
  (:require gdl.context
            game.context))

; TODO grep source/target
; TODO effects/damage ? namespaced ? maybe hit-effects/ bla-effects/

(defn- type-dispatch [_context [type _value]]
  type)

; TODO everywhere not value but type value passed
;; TODO search game.effect, not exist anymore...

(defmulti do!           type-dispatch)
(defmulti text          type-dispatch)
(defmulti valid-params? type-dispatch)

(defmulti render-info type-dispatch)
(defmethod render-info :default [_ _])

(defmulti useful? type-dispatch)
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
