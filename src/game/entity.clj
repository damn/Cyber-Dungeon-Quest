(ns game.entity
  (:require [x.x :refer [defsystem]]))

; TODO always context last param

(defsystem create [_])
(defsystem create! [_ e context])

(defsystem destroy [_])
(defsystem destroy! [_ e context])

(defsystem tick  [_ delta])
(defsystem tick! [_ context e delta])

(defsystem moved! [_ e direction-vector])
(defsystem affected! [_ e])
(defsystem stun! [_ e])

(defsystem render-below   [_ context e*])
(defsystem render-default [_ context e*])
(defsystem render-above   [_ context e*])
(defsystem render-info    [_ context e*])
(defsystem render-debug   [_ context e*])
