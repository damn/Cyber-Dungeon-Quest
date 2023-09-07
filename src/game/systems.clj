(ns game.systems
  (:require [x.x :refer [defsystem]]))

; game.render

(comment
 ; two times underscore => the let does not work anymore ! shadowed...
 ; => check if first element shadowed
 ; => warning on shadow first arg which I am let-ting over.
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

; game.effects.stun

(defsystem stun!         [c e])

; TODO add session / counter (stopped) / counter/animation reset ?
; 'verbs'
