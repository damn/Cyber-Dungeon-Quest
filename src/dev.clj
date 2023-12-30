(ns dev
  (:require [clojure.pprint :refer :all]
            [clojure.string :as str]))

(comment
 (let [ctx @gdl.app/current-context
       entity (cdq.context/get-entity ctx 2)
       ]

   (clojure.pprint/pprint
    (sort
     (keys @entity)))

   ))

(defn- get-namespaces []
   (filter #(#{"data" "game" "mapgen" "property-editor" "utils"}
             (first (str/split (name (ns-name %)) #"\.")))
           (all-ns)))

(defn- get-non-fn-vars [nmspace]
   (for [[sym avar] (ns-interns nmspace)
         :let [value @avar]
         :when (not (or (fn? value)
                        (instance? clojure.lang.MultiFn value)
                        #_(:method-map value) ; breaks for stage Ilookup
                        ))]
     avar))

(comment
 (gdl.backends.libgdx.dev/restart!)

 (spit "app-values-tree.clj"
       (with-out-str
        (pprint
         (for [nmspace (sort-by (comp name ns-name)
                                (get-namespaces))
               :let [value-vars (get-non-fn-vars nmspace)]
               :when (seq value-vars)]
           [(ns-name nmspace) (map (comp symbol name symbol) value-vars)]))))


 (require '[cdq.context :refer [get-entity]])
 (let [entity* @(get-entity @gdl.app/current-context 49)]
   (:mana entity*)
   )

 )
