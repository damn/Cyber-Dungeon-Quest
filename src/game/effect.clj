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

(defn- do-effect!* [effect params]
  {:pre [(valid-params? effect params)]}
  (call-effect-fn :do! effect params))

(defn- trigger-affected! [target]
  (when target
    (doseq-entity target entity/affected!)))

(defn do! [effect params]
  (do-effect!* effect params)
  (trigger-affected! (:target params)))

(defn do-all! [effects params]
  (doseq [effect effects]
    (do-effect!* effect params))
  (trigger-affected! (:target params)))

(defmulti render-info (fn [[effect-type effect-value] effect-params] effect-type))
(defmethod render-info :default [_ _])
