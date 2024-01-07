(ns cdq.context.modifier
  (:require [clojure.string :as str]
            gdl.context
            cdq.context))

(def modifier-definitions {})

(defn defmodifier [k modifier]
  (alter-var-root #'modifier-definitions assoc k modifier)
  k)

(defn- text [[modifier-type value]]
  ((:text (modifier-type modifier-definitions))
   value))

(extend-type gdl.context.Context
  cdq.context/Modifier
  (modifier-text [_ modifier]
    (->> (for [component modifier]
           (text component))
         (str/join "\n"))))

(defn- modifier-tx [fn-k entity [modifier-key modifier-value]]
  {:pre [(#{:apply :reverse} fn-k)]}
  (let [modifier-def (get modifier-definitions modifier-key)
        modify (fn-k modifier-def)
        ks (:keys modifier-def)]
    (assert modifier-def (str "Could not find modifier: " modifier-key))
    [:tx/assoc-in entity ks (modify (get-in @entity ks) modifier-value)]))

(defn- gen-txs [fn-k entity modifier]
  (for [component modifier]
    (modifier-tx fn-k entity component)))

(defmethod cdq.context/transact! :tx/apply-modifier [[_ entity modifier] ctx]
  (gen-txs :apply entity modifier))

(defmethod cdq.context/transact! :tx/reverse-modifier [[_ entity modifier] ctx]
  (gen-txs :reverse entity modifier))
