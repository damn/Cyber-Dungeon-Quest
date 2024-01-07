(ns cdq.context.modifier
  (:require [clojure.string :as str]
            gdl.context
            cdq.context))

(def modifier-definitions {})

(defn defmodifier [k modifier]
  (alter-var-root #'modifier-definitions assoc k modifier)
  k)

(defn- call-modifier-fn [fn-k entity* [modifier-key modifier-value]]
  {:pre [(#{:apply :reverse} fn-k)]}
  (let [modifier-def (get modifier-definitions modifier-key)
        modify (fn-k modifier-def)]
    (assert modifier-def (str "Could not find modifier: " modifier-key))
    (update-in entity* (:keys modifier-def) modify modifier-value)))

(defn- reduce-modifier [entity* fn-k modifier]
  (reduce (partial call-modifier-fn fn-k)
          entity*
          modifier))

(defn- apply-modifier   [entity* modifier] (reduce-modifier entity* :apply   modifier))
(defn- reverse-modifier [entity* modifier] (reduce-modifier entity* :reverse modifier))

(defn- text [[modifier-type value]]
  ((:text (modifier-type modifier-definitions))
   value))

(defmethod cdq.context/transact! :tx/apply-modifier [[_ entity modifier] ctx]
  [(apply-modifier @entity modifier)])

(defmethod cdq.context/transact! :tx/reverse-modifier [[_ entity modifier] ctx]
  [(reverse-modifier @entity modifier)])

(extend-type gdl.context.Context
  cdq.context/Modifier
  (modifier-text [_ modifier]
    (->> (for [component modifier]
           (text component))
         (str/join "\n"))))
