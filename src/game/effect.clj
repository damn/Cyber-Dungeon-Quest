(ns game.effect)

(def ^:private effect-definitions {})

(defn defeffect [effect-type effect-def]
  (alter-var-root #'effect-definitions assoc effect-type effect-def)
  effect-type)

(defn- call-effect-fn [fn-k [effect-type effect-value] params]
  (let [effect-def (effect-type effect-definitions)]
    (assert effect-def (str "Effect " effect-type " not defined."))
    ((fn-k effect-def) effect-value params)))

(def text          (partial call-effect-fn :text))
(def valid-params? (partial call-effect-fn :valid-params?))

(defn- do-effect!* [effect params context]
  {:pre [(valid-params? effect params)]}
  (let [[effect-type effect-value] effect
        effect-def (effect-type effect-definitions)]
    (assert effect-def (str "Effect " effect-type " not defined."))
    ((:do! effect-def) effect-value params context)))

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

