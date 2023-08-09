; TODO move to gdl ?
; defmedia ?
; media/ ?
(ns game.media
  (:require [x.temp :as app :refer [set-var-root dispose]]
            [gdl.files :as files]
            [gdl.graphics.image :as image]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.freetype :as freetype]))

(declare font)

(app/on-create
 (set-var-root #'font
               (freetype/generate (files/internal "exocet/films.EXL_____.ttf")
                                  16)))

(app/on-destroy
 (dispose font))

; spritesheet sprite position starts start top - left !

; TODO move idx in here not in separate files
; monster only needs to know 'elfen archer picture' and not the index
; proper abstraction
; -> keywords
; :fx/blood
; :impact-fx/scratch
; :creatures/vampire
; -> could load it as a tileset also and save there info which keyword which
; tile
; the tileseet could also have property to say how many frames each animation.
; -> easy spritesheet index management
; -> can easily reskin without touching monster definition
; -> ask question : does this need to know about that ?
; -> or : minimal change touching
; -> all UI skin / sizes defined somewhere, can easily reskin UI everything
; for different games


(app/defmanaged ^:private fx        (image/spritesheet "fx/uf_FX.png"        24 24))
(app/defmanaged ^:private fx-impact (image/spritesheet "fx/uf_FX_impact.png" 48 48))

; TODO do I share animations in vars and use across different entities ?
; -> check !
; -> load frames here (already loaded !)
; -> no need to set in a var

(defn blood-animation []
  (animation/create (map #(image/get-sprite fx [(+ 4 %) 6]) (range 6))
                    :frame-duration 50))

(defn plop-animation []
  (animation/create (map #(image/get-sprite fx [% 7]) (range 6))
                    :frame-duration 25))

(defn black-projectile []
  (animation/create [(image/get-sprite fx [1 12])]
                    :frame-duration 500))

(defn red-explosion-animation []
  (animation/create (map #(image/get-sprite fx [% 1]) (range 5))
                    :frame-duration 50))

(defn fx-impact-animation [[x y]]
  (animation/create [(image/get-sprite fx-impact [x       y])
                     (image/get-sprite fx-impact [(+ x 1) y])
                     (image/get-sprite fx-impact [(+ x 2) y])]
                    :frame-duration 50))
