(ns x.systems ; TODO all x => game.
  (:require [x.x :refer [defsystem]]))

; TODO move systems where they are used !
; no HUGE BIG SYSTEMS NS

; game.tick

(defsystem tick          [c delta])
(defsystem tick!         [c e delta])

; game.render

(comment
 ; two times underscore => the let does not work anymore ! shadowed...
 ; => check if first element shadowed => warning...
 (clojure.core/defmethod
   render
   :line-render
   [_ _ position]
   (clojure.core/let
     [{:keys [thick? end color]} (_ 1)]
     (if
       thick?
       (shape-drawer/with-line-width
         4
         (shape-drawer/line position end color))
       (shape-drawer/line position end color))))
 :line-render)


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
