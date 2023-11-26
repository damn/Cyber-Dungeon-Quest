(ns game.media
  (:require [x.x :refer [defmodule]]
            [gdl.utils :refer [dispose]]
            [gdl.lc :as lc]
            [gdl.files :as files]
            [gdl.graphics.image :as image]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.freetype :as freetype]))

(declare font ^:private fx ^:private fx-impact)

(defmodule _
  (lc/create [_]
    (.bindRoot #'font (freetype/generate (files/internal "exocet/films.EXL_____.ttf") 16))
    (.bindRoot #'fx        (image/spritesheet "fx/uf_FX.png"        24 24))
    (.bindRoot #'fx-impact (image/spritesheet "fx/uf_FX_impact.png" 48 48)))
  (lc/dispose [_]
    (dispose font)))

; spritesheet sprite position starts top-left

(defn blood-animation []
  (animation/create (map #(image/get-sprite fx [(+ 4 %) 6]) (range 6)) :frame-duration 50))

(defn plop-animation []
  (animation/create (map #(image/get-sprite fx [% 7]) (range 6)) :frame-duration 25))

(defn black-projectile [] ; todo why animation ? only 1 frame
  (animation/create [(image/get-sprite fx [1 12])] :frame-duration 500))

#_(defn red-explosion-animation []
    (animation/create (map #(image/get-sprite fx [% 1]) (range 5)) :frame-duration 50))

(defn fx-impact-animation [[x y]]
  (animation/create [(image/get-sprite fx-impact [x       y])
                     (image/get-sprite fx-impact [(+ x 1) y])
                     (image/get-sprite fx-impact [(+ x 2) y])]
                    :frame-duration 50))
