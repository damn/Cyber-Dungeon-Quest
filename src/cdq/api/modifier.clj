(ns cdq.api.modifier
  (:refer-clojure :exclude [keys apply reverse])
  (:require [x.x :refer [defsystem]]))

(defsystem text [_])
(defsystem keys [_])
(defsystem apply   [_ value])
(defsystem reverse [_ value])
