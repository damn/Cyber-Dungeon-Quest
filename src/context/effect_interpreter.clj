(ns context.effect-interpreter)

(def ^:private components {})

(defn component [effect-type effect-def]
  (alter-var-root #'components assoc effect-type effect-def)
  effect-type)

; TODO this is just multimethods without default ? possible? that they then break ?
; defsystems / component / defsystem without default return ?
; always  give explicit default return? just use defmultis?
(defn- call-effect-fn [fn-key context [type value] params context]
  (let [definition (get components type)]
    (assert definition (str "Effect " type " not defined."))
    ((fn-key definition) value params context)))

; only used @ skill, 1-part-effects only yet.
(def valid-params? (partial call-effect-fn :valid-params?))

(defn text [context effect params]
  (->> (for [component effect]
         (call-effect-fn :text context component params))
       (str/join "\n")))

(defn do! [context effect params]
  ; (assert (valid-params? context component params)) ; TODO checking line of sight, etc again here , should already be checked
  (doseq [component effect]
    (call-effect-fn :do! context component params)))

; these are context-systems O.O just multimethods ....

(defmulti render-info (fn [_context [type _value]] type))
(defmethod render-info :default [_ _])

(defmulti ai-should-use? (fn [context [type _value]] type))
(defmethod ai-should-use? :default [_ _]
  true) ; TODO ?!
