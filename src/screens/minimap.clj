(ns screens.minimap
  (:require [gdl.app :as app]
            gdl.screen
            [gdl.maps.tiled :as tiled]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.draw :as draw]
            [game.maps.tile-color-setters :refer [minimap-color-setter]])
  (:import (com.badlogic.gdx Gdx Input$Keys)
           (com.badlogic.gdx.graphics Color OrthographicCamera)))

; 28.4 viewportwidth
; 16 viewportheight
; camera shows :
;  [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
; zoom default '1'
; zoom 2 -> shows double amount

; we want min/max explored tiles X / Y and show the whole explored area....

(def ^:private zoom-setting (atom nil))

(defn- calculate-zoom [{:keys [^OrthographicCamera world-camera
                               context/world-map]}]
  (let [positions-explored (map first
                                (remove (fn [[position value]]
                                          (false? value))
                                        (seq @(:explored-tile-corners world-map))))
        viewport-width  (.viewportWidth  world-camera)
        viewport-height (.viewportHeight world-camera)
        [px py] (camera/position world-camera)
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

(deftype Screen []
  gdl.screen/Screen
  (show [_ {:keys [world-camera] :as context}]
    (reset! zoom-setting (calculate-zoom context))
    (camera/set-zoom! world-camera @zoom-setting))
  (hide [_ _ctx])
  (render [_ {:keys [world-camera context/world-map] :as context}]
    (tiled/render-map context
                      (:tiled-map world-map)
                      minimap-color-setter)
    (app/render-view context
                     :world
                     (fn [drawer]
                       (draw/filled-circle drawer (camera/position world-camera) 0.5 Color/GREEN))))
  (tick [_ {:keys [world-camera]} delta]
    (when (or (.isKeyJustPressed Gdx/input Input$Keys/TAB)
              (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE))
      (camera/set-zoom! world-camera 1)
      (app/change-screen! :screens/game))))
