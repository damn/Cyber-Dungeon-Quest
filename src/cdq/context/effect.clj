(ns cdq.context.effect
  (:require [clojure.string :as str]
            gdl.context
            cdq.context))

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
  cdq.context/EffectInterpreter
  (do-effect! [context effect]
    (assert (cdq.context/valid-params? context effect)) ; extra line of sight checks TODO performance issue?
    (doseq [component effect]
      (do! context component)))

  (effect-text [context effect]
    (->> (keep #(text context %) effect)
         (str/join "\n")))

  (valid-params? [context effect]
    (every? (partial valid-params? context) effect))

  (effect-render-info [context effect]
    (doseq [component effect]
      (render-info context component)))

  (effect-useful? [context effect]
    (some (partial useful? context) effect)))
