(ns game.effect
  (:require [x.x :refer [doseq-entity]]
            [game.entity :as entity]))

(comment
 ; todo first step change value/params/effect param order
 ; and 2. make effects/ namespaced kw. => resources/properties.edn has effects
 ; 3. also required-params ? just data ?
 ; 4. system as protocol -> defsystems -> all required to implement possible?
 ; why not defsystems just defines multiple , but then need a name for it?
 (defsystem component-text [_ params])
 (defsystem valid-params? [_ params]) ; can also check component value itself ?
 ; => spec / malli spec fn also ?
 (defsystem component-do [_ params]))

(def ^:private effect-definitions {})

(defn defeffect [effect-type effect-def] ; TODO just 'def'
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

(defn- trigger-affected! [target context]
  (when target
    (doseq-entity target entity/affected! context)))

(defn do! [effect params context]
  (do-effect!* effect params context)
  (trigger-affected! (:target params) context))

(defn do-all! [effects params context]
  (doseq [effect effects]
    (do-effect!* effect params context))
  (trigger-affected! (:target params) context))

(defmulti render-info (fn [drawer [effect-type effect-value] effect-params] effect-type))
(defmethod render-info :default [_ _ _])
