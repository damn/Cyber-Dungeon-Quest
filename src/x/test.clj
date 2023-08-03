(clojure.core/use 'nstools.ns)

(ns+ x.test
     (:like x.ns))

(comment
 (db/get-entity 1)
 (g/*unit-scale*))

