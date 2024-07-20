(ns screens.minimap
  (:require [gdl.app :refer [current-context change-screen!]]
            gdl.screen
            [gdl.input.keys :as input.keys]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.context :refer [draw-filled-circle render-world-view key-just-pressed? render-tiled-map]]
            [cdq.api.context :refer [explored?]]))

; 28.4 viewportwidth
; 16 viewportheight
; camera shows :
;  [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
; zoom default '1'
; zoom 2 -> shows double amount

; we want min/max explored tiles X / Y and show the whole explored area....

(defn- calculate-zoom [{:keys [world-camera
                               context/world]}]
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
  (show [_ {:keys [world-camera] :as context}]
    (camera/set-zoom! world-camera (calculate-zoom context)))

  (hide [_ {:keys [world-camera]}]
    (camera/reset-zoom! world-camera))

  (render [_ {:keys [world-camera context/world] :as context}]
    (render-tiled-map context (:tiled-map world) tile-corner-color-setter)
    (render-world-view context
                       #(draw-filled-circle % (camera/position world-camera) 0.5 color/green))
    (when (or (key-just-pressed? context input.keys/tab)
              (key-just-pressed? context input.keys/escape))
      (change-screen! :screens/game))))
