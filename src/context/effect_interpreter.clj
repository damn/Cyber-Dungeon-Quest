(ns context.effect-interpreter
  (:require gdl.context
            game.context))

; TODO grep source/target

(def ^:private components {})

(defn component [effect-type effect-def]
  (alter-var-root #'components assoc effect-type effect-def)
  effect-type)

(defn- call-effect-fn [fn-key context [type value]]
  (let [definition (get components type)]
    (assert definition (str "Effect " type " not defined."))
    ((fn-key definition) context value)))

(defn- type-dispatch [_context [type _value]]
  type)

(defmulti render-info type-dispatch)
(defmethod render-info :default [_ _])

(defmulti useful? type-dispatch)
(defmethod useful? :default [_ _] true)

(extend-type gdl.context.Context
  game.context.EffectInterpreter
  (do-effect! [context effect]
    ; (assert (valid-params? context effect)) ; TODO checking line of sight, etc again here , should already be checked
    (doseq [component effect]
      (call-effect-fn :do! context component)))

  (effect-text [context effect]
    (->> (for [component effect]
           (call-effect-fn :text context component))
         (str/join "\n")))

  (valid-params? [context effect]
    ; TODO only used @ skill, 1-part-effects only yet.
    (call-effect-fn :valid-params? context effect))

  (effect-render-info [context effect]
    ; TODO only used @ skill, 1-part-effects only yet.
    (render-info context effect))

  (effect-useful? [_ effect]
    ; TODO only used @ skill, 1-part-effects only yet.
    (useful? context effect)))
