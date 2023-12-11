(ns game.media
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.files :as files]
            [gdl.graphics.image :as image]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.freetype :as freetype]))

(declare font
         ^:private fx)

(defmodule _
  (lc/create [_ {:keys [assets]}]
    (.bindRoot #'font (freetype/generate (files/internal "exocet/films.EXL_____.ttf") 16))
    (.bindRoot #'fx (image/spritesheet assets "fx/uf_FX.png" 24 24)))
  (lc/dispose [_]
    (dispose ^com.badlogic.gdx.graphics.g2d.BitmapFont font)))

; TODO do projectiles why animation ? only 1 frame
(defn black-projectile [assets]
  (animation/create [(image/get-sprite assets fx [1 12])] :frame-duration 500))
