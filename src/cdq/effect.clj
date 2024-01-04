(ns cdq.effect)

; TODO no assert needed ?
; is throwing on no impl ?
; => always
(defn- by-type [_context [type value]]
  (assert (keyword? type)
          (str "Type is not a keyword: " type " and value: " value))
  (assert (= "effect" (namespace type))
          (str "Effect keys need to have :effect/ keyword namespace type: " type " , value: " value))
  type)

(defmulti do!           by-type)
(defmulti text          by-type)
(defmulti valid-params? by-type)
(defmulti value-schema  identity)

(defmulti render-info   by-type)
(defmethod render-info :default [_ _])

(defmulti useful?       by-type)
(defmethod useful? :default [_ _] true)
