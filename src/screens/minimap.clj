(ns screens.minimap
  (:require [gdl.app :refer [current-context change-screen!]]
            [gdl.context :refer [key-just-pressed?]]
            [gdl.graphics :as g]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.input.keys :as input.keys]
            gdl.screen
            [cdq.api.context :refer [explored?]]))

; 28.4 viewportwidth
; 16 viewportheight
; camera shows :
;  [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
; zoom default '1'
; zoom 2 -> shows double amount

; we want min/max explored tiles X / Y and show the whole explored area....

(defn- calculate-zoom [{:keys [context/world]
                        {:keys [world-camera]} :context/graphics}]
  (let [positions-explored (map first
                                (remove (fn [[position value]]
                                          (false? value))
                                        (seq @(:explored-tile-corners world))))
        left   (apply min-key (fn [[x y]] x) positions-explored)
        top    (apply max-key (fn [[x y]] y) positions-explored)
        right  (apply max-key (fn [[x y]] x) positions-explored)
        bottom (apply min-key (fn [[x y]] y) positions-explored)]
    (camera/calculate-zoom world-camera
                           :left left
                           :top top
                           :right right
                           :bottom bottom)))

; TODO FIXME deref'fing current-context at each tile corner
; massive performance issue - probably
; => pass context through java tilemap render class
; or prepare colors before
(defn- tile-corner-color-setter [color x y]
  (if (explored? @current-context [x y])
    color/white
    color/black))

(deftype Screen []
  gdl.screen/Screen
  (show [_ {{:keys [world-camera]} :context/graphics :as ctx}]
    (camera/set-zoom! world-camera (calculate-zoom ctx)))

  (hide [_ {{:keys [world-camera]} :context/graphics}]
    (camera/reset-zoom! world-camera))

  (render [_ {{:keys [world-camera] :as g} :context/graphics
              :keys [context/world] :as context}]
    (g/render-tiled-map g (:tiled-map world) tile-corner-color-setter)
    (g/render-world-view g
                         (fn [g]
                           (g/draw-filled-circle g (camera/position world-camera) 0.5 color/green)))
    (when (or (key-just-pressed? context input.keys/tab)
              (key-just-pressed? context input.keys/escape))
      (change-screen! :screens/game))))
