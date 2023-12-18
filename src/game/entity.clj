(ns game.entity ; TODO rename to game.component
  (:require [x.x :refer [defsystem]]))

; e = entity reference (an atom)
; e* = deref-ed entity, a map.
; c = context

; TODO always context last param

(defsystem create [_])
(defsystem create! [_ e c])

(defsystem destroy [_])
(defsystem destroy! [_ e c])

(defsystem tick  [_ delta])
(defsystem tick! [_ c e delta])

(defsystem moved! [_ e c direction-vector])

(defsystem render-below   [_ c e*])
(defsystem render-default [_ c e*])
(defsystem render-above   [_ c e*])
(defsystem render-info    [_ c e*])
(defsystem render-debug   [_ c e*])
