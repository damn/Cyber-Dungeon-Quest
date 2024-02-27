(ns cdq.context.modifier
  (:require [clojure.string :as str]
            gdl.context
            [cdq.context :refer [transact!]]
            [cdq.modifier :as modifier]))

(extend-type gdl.context.Context
  cdq.context/Modifier
  (modifier-text [_ modifier]
    (->> (for [component modifier]
           (modifier/text component))
         (str/join "\n"))))

(defn- gen-txs [system entity modifier]
  (for [component modifier
        :let [ks (modifier/keys component)]]
    [:tx/assoc-in entity ks (system component (get-in @entity ks))]))

(defmethod transact! :tx/apply-modifier [[_ entity modifier] ctx]
  (gen-txs modifier/apply entity modifier))

(defmethod transact! :tx/reverse-modifier [[_ entity modifier] ctx]
  (gen-txs modifier/reverse entity modifier))
