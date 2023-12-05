(ns dev
  (:require [clojure.pprint :refer :all]
            [clojure.string :as str]))

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
 (spit "app-values-tree.clj"
       (with-out-str
        (pprint
         (for [nmspace (sort-by (comp name ns-name)
                                (get-namespaces))
               :let [value-vars (get-non-fn-vars nmspace)]
               :when (seq value-vars)]
           [(ns-name nmspace) (map (comp symbol name symbol) value-vars)])))))