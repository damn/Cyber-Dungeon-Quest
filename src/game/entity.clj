(ns game.entity
  (:require [x.x :refer [defsystem]]))

(defsystem create [_])
(defsystem create! [_ e])

(defsystem destroy [_])
(defsystem destroy! [c e])

(defsystem tick  [_ delta])
(defsystem tick! [_ e delta])

(defsystem moved! [_ e direction-vector])
(defsystem affected! [_ e])
(defsystem stun! [_ e])

(defsystem render-below   [_ context e*])
(defsystem render-default [_ context e*])
(defsystem render-above   [_ context e*])
(defsystem render-info    [_ context e*])
(defsystem render-debug   [_ context e*])
