(ns cdq.effect
  (:require [x.x :refer [defsystem]]))

(defsystem value-schema  [_])

(defsystem text          [_ ctx])

(defsystem valid-params? [_ ctx])

(defsystem useful?       [_ ctx])
(defmethod useful? :default [_ _] true)

(defsystem transactions  [_ ctx])

(defsystem render-info   [_ ctx])
(defmethod render-info :default [_ _])
