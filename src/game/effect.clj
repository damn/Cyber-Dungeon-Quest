(ns game.effect
  (:require [x.x :refer :all]))

(def ^:private effect-definitions {})

(defn defeffect [effect-type effect-def]
  (alter-var-root #'effect-definitions assoc effect-type effect-def)
  effect-type)

(defn- call-effect-fn [fn-k params [type value]]
  (let [effect (type effect-definitions)
        params (assoc params :value value)
        f (fn-k effect)]
    (assert effect (str "Effect " type " not defined."))
    (f params)))

(def text          (partial call-effect-fn :text))
(def valid-params? (partial call-effect-fn :valid-params?))

(defn- do-effect!* [params effect]
  {:pre [(valid-params? params effect)]}
  (call-effect-fn :do! params effect))

(defsystem affected! [c e])

(defn- trigger-affected! [target]
  (when target
    (doseq-entity target affected!)))

(defn do! [params effect]
  (do-effect!* params effect)
  (trigger-affected! (:target params)))

(defn do-all! [params effects]
  (doseq [effect effects]
    (do-effect!* params effect))
  (trigger-affected! (:target params)))
