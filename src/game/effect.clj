(ns game.effect)

(def ^:private effect-definitions {})

(defn defeffect [effect-type effect-def]
  (alter-var-root #'effect-definitions assoc effect-type effect-def)
  effect-type)

; TODO this is just multimethods without default ? possible? that they then break ?
; defsystems / component / defsystem without default return ?
; always  give explicit default return? just use defmultis?
(defn- call-effect-fn [fn-key effect params context]
  (let [[type value] effect
        definition (get effect-definitions type)]
    (assert definition (str "Effect " type " not defined."))
    ((fn-key definition) value params context)))

(def text          (partial call-effect-fn :text))
(def valid-params? (partial call-effect-fn :valid-params?))

(defn- do-effect!* [effect params context]
  {:pre [(valid-params? effect params context)]} ; TODO checking line of sight, etc again here , should already be checked
  (call-effect-fn :do! effect params context))

(defn do! [effect params context]
  (do-effect!* effect params context))

(defn do-all! [effects params context]
  (doseq [effect effects]
    (do-effect!* effect params context)))

(defmulti render-info (fn [c [effect-type effect-value] effect-params]
                        effect-type))
(defmethod render-info :default [_ _ _])

(defmulti ai-should-use? (fn [[effect-type _effect-value] _effect-params _context _entity*]
                           effect-type))
(defmethod ai-should-use? :default [_ _effect-params _context _entity*]
  true)

