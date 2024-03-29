(ns cdq.effect
  (:require [x.x :refer [defsystem]]))

(defsystem text          [_ ctx])
(defmethod text :default [_ _])

(defsystem valid-params? [_ ctx])
(defmethod valid-params? :default [_ _] true)

(defsystem useful?       [_ ctx])
(defmethod useful? :default [_ _] true)

(defsystem render-info   [_ ctx])
(defmethod render-info :default [_ _])
