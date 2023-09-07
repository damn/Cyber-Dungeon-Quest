(ns game.effects.core
  (:require [x.x :refer :all]))

(def effect-definitions {})

(defn defeffect [effect-type effect-def]
  (alter-var-root #'effect-definitions assoc effect-type effect-def)
  effect-type)

(defn- call-effect-fn [fn-k params [type value]]
  (let [effect (type effect-definitions)
        params (assoc params :value value)
        f (fn-k effect)]
    (assert effect (str "Effect " type " not defined."))
    (f params)))

; TODO can effects also be entites w. components ?
; ??
;; where we have sytems 'text', 'do!', 'valid-params?' ??

; different params are also different how they throw errors etc.
; they can also be components again ??
; just connect w. data
; e.g. 'line-of-sight' param condition -> is a keyword & behaviour .. ??

; modifier also [k v] ...
; so modifier are component
; and modifiers == just coll of  components
; modifier calls apply/reverse only
; modifier system

; protocols/systems/? many things have 'text', even entities
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
