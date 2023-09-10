; TODO effect = has effect COMPONENTS
; => one 'effect' consists of multiple effect-components
; use modules/namespaced keywords?
; make module e.g. ns effect/restoration.clj => not effects.restoration but
; :effect/restoration
; => remove game folder => just 'effect' ns ?
; TODO delete do-effects! / do-effect! => effect has always multiple components?
; => just do-effect => one effect has multiple effect-components (always?)
; => wrap in one more vector wherevr or in map
; => do-effect-component internal function
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
(def ^:private do! (partial call-effect-fn :do!))

(defn- do-effect!* [params effect]
  {:pre [(valid-params? params effect)]}
  (do! params effect))

(defsystem affected! [c e])

(defn- trigger-affected! [target]
  (when target
    (doseq-entity target affected!)))

(defn do-effect! [params effect]
  (do-effect!* params effect)
  (trigger-affected! (:target params)))

(defn do-effects! [params effects]
  (doseq [effect effects]
    (do-effect!* params effect))
  (trigger-affected! (:target params)))
