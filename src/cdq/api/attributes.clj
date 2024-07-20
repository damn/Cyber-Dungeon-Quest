(ns cdq.api.attributes)

(def attributes {})

(defn defattribute [k data]
  (assert (:schema data) k)
  (assert (:widget data) k)
  ; optional: :doc
  (alter-var-root #'attributes assoc k data))

; TODO this is == :optional key @ components-attribute ?
(defn removable-component? [k]
  (#{"tx" "modifier" #_"entity"} (namespace k)))
