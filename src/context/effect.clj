(ns context.effect
  (:require [clojure.string :as str]
            gdl.context
            game.context))

; TODO spell effect-text only effect/source
; @ game.skill/text ... o.o,need to know that @ texts
; TODO no default - make sure is no typo - better have to implement all ?
; => make macro defmethods
; TODO @do-effecT! validparam check
; checking line of sight, etc again here , should already be checked
; maybe just checks required-keys , list of required keys
; we also need to check the effect-value too (for property editor also)

(defn- by-type [_context [type value]]
  (assert (keyword? type)
          (str "Type is not a keyword: " type " and value: " value))
  (assert (= "effect" (namespace type))
          (str "Effect keys need to have :effect/ keyword namespace type: " type " , value: " value))
  type)

(defmulti do!           by-type)
(defmulti text          by-type)
(defmulti valid-params? by-type)

(defmulti render-info   by-type)
(defmethod render-info :default [_ _])

(defmulti useful?       by-type)
(defmethod useful? :default [_ _] true)

(extend-type gdl.context.Context
  game.context/EffectInterpreter
  (do-effect! [context effect]
    (assert (game.context/valid-params? context effect))
    (doseq [component effect]
      (do! context component)))

  (effect-text [context effect]
    (->> (for [component effect]
           (text context component))
         (str/join "\n")))

  (valid-params? [context effect]
    (every? (partial valid-params? context) effect))

  (effect-render-info [context effect]
    (doseq [component effect]
      (render-info context component)))

  (effect-useful? [context effect]
    (some (partial useful? context) effect)))
