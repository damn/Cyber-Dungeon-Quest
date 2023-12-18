(ns game.entity ; TODO rename to game.component
  (:require [x.x :refer [defsystem]]))

; TODO always context last param

(defsystem create [_])
(defsystem create! [_ e context])

(defsystem destroy [_])
(defsystem destroy! [_ e context])

(defsystem tick  [_ delta])
(defsystem tick! [_ context e delta])

(defsystem moved! [_ e context direction-vector])

(defsystem render-below   [_ drawer context e*])
(defsystem render-default [_ drawer context e*])
(defsystem render-above   [_ drawer context e*])
(defsystem render-info    [_ drawer context e*])
(defsystem render-debug   [_ drawer context e*])
