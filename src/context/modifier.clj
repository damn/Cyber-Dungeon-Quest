(ns context.modifier
  (:require [clojure.string :as str]
            gdl.context
            cdq.context))

(def modifier-definitions {})

(defn defmodifier [k modifier]
  (alter-var-root #'modifier-definitions assoc k modifier)
  k)

; TODO assert, do like effect

; Example:
; (call-modifier-fn :apply entity* [:mana 30])
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

(extend-type gdl.context.Context
  cdq.context/Modifier
  (apply-modifier! [_ entity modifier]
    (swap! entity apply-modifier modifier))

  (reverse-modifier! [_ entity modifier]
    (swap! entity reverse-modifier modifier))

  (modifier-text [_ modifier]
    (->> (for [component modifier]
           (text component))
         (str/join "\n"))))
