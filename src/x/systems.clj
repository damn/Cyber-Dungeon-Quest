(ns x.systems
  (:require [x.x :refer [defsystem]]))

; TODO move systems where they are used !
; no HUGE BIG SYSTEMS NS

; x.db

(defsystem create        [c])
(defsystem create!       [c e])
(defsystem after-create! [c e])

(defsystem destroy       [c])
(defsystem destroy!      [c e])

; game.tick

(defsystem tick          [c delta])
(defsystem tick!         [c e delta])

; game.render

(defsystem render-below  [c m position]) ; entity effects, mouseover-outline
(defsystem render        [c m position]) ; image, animation
(defsystem render-above  [c m position]) ; psi-charges, glittering, shield ( == 'effects' ?)
(defsystem render-info   [c m position]) ; hp-bar, attacking-arc

(defsystem render-debug  [c m position]) ; body-bounds, mouseover entity info

; game.components.movement

(defsystem moved         [c direction-vector])
(defsystem moved!        [c e])

; game.effects.core

(defsystem affected!     [c e])

; game.effects.stun

(defsystem stun!         [c e])

; TODO add session / counter (stopped) / counter/animation reset ?
; 'verbs'
