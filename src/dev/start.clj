(ns dev.start
  (:require [clojure.tools.namespace.repl :refer [disable-reload!]]))

(disable-reload!)

(comment
 (gdl.dev-loop/restart!)
 )

(comment


 (use 'clojure.pprint)
 (require '[clojure.string :as str])

 (defn get-namespaces []
   (filter #(#{"game" "gdl" "mapgen" "x"}
             (first (str/split (name (ns-name %)) #"\.")))
           (all-ns)))

 (defn get-non-fn-vars [nmspace]
   (for [[sym avar] (ns-interns nmspace)
         :let [value @avar]
         :when (not (or (fn? value)
                        (instance? clojure.lang.MultiFn value)
                        (:method-map value)
                        (instance? x.session.State value)))]
     avar))

 (def app-values-tree
   (for [nmspace (sort-by (comp name ns-name)
                          (get-namespaces))
         :let [value-vars (get-non-fn-vars nmspace)]
         :when (seq value-vars)]
     [(ns-name nmspace) (map (comp symbol name symbol) value-vars)]))

 (spit "app-values-tree.clj" (with-out-str (pprint app-values-tree)))

 (supers (class @(var x.session/State)))

 (instance? x.session.State x.db/db-state )

 )
