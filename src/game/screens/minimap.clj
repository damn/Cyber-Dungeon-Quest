(ns game.screens.minimap
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.lc :as lc]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.world :as world]
            [game.utils.lightning :refer [minimap-color-setter]]
            [game.maps.data :refer [get-current-map-data]])
  (:import com.badlogic.gdx.graphics.Color))

; 28.4 viewportwidth
; 16 viewportheight
; camera shows :
;  [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
; zoom default '1'
; zoom 2 -> shows double amount

; we want min/max explored tiles X / Y and show the whole explored area....

(def ^:private zoom-setting (atom nil))

(defn- calculate-zoom []
  (let [positions-explored (map first
                                (remove (fn [[position value]]
                                          (false? value))
                                        (seq @(:explored-tile-corners (get-current-map-data)))))
        camera world/camera
        viewport-width (.viewportWidth camera)
        viewport-height (.viewportHeight camera)
        [px py] (world/camera-position)
        left   (apply min-key (fn [[x y]] x) positions-explored)
        top    (apply max-key (fn [[x y]] y) positions-explored)
        right  (apply max-key (fn [[x y]] x) positions-explored)
        bottom (apply min-key (fn [[x y]] y) positions-explored)
        x-diff (max (- px (left 0)) (- (right 0) px))
        y-diff (max (- (top 1) py) (- py (bottom 1)))
        vp-ratio-w (/ (* x-diff 2) viewport-width)
        vp-ratio-h (/ (* y-diff 2) viewport-height)
        new-zoom (max vp-ratio-w vp-ratio-h)]
    new-zoom ))

; TODO move to gdl.graphics - also in mapgen.tiledmap-renderer
(defn- set-zoom [amount]
  (let [camera world/camera] ; keep it public why not?!
    (set! (.zoom camera) amount)
    (.update camera)))

; TODO for debugging I want to zoom -> InputAdapter scroll/touch/gestures ??
; (set! (.zoom @#'gdl.graphics/world-camera) 0.5)

(defn- render-map-level [_context]
  (shape-drawer/filled-circle (world/camera-position) 0.5 Color/GREEN)) ; render player..

(defmodule _
  (lc/show [_]
    (reset! zoom-setting (calculate-zoom))
    (set-zoom @zoom-setting))
  (lc/render [_ context]
    (tiled/render-map context
                      (:tiled-map (get-current-map-data))
                      minimap-color-setter)
    (app/render-with context
                     :world
                     render-map-level))
  (lc/tick [_ _state delta]
    (when (or (input/is-key-pressed? :TAB)
              (input/is-key-pressed? :ESCAPE))
      (set-zoom 1)
      (app/set-screen :game.screens.ingame))))
