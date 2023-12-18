(ns game.render.debug
  (:require [gdl.app :as app]
            [gdl.protocols :refer [draw-circle draw-rectangle draw-filled-rectangle draw-grid]]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [game.maps.cell-grid :as cell-grid])
  (:import com.badlogic.gdx.graphics.Color))

; TODO make check-buttons with debug-window or MENU top screen is good for debug I think

(defn- geom-test [{:keys [world-mouse-position context/world-map] :as c}]
  (let [position world-mouse-position
        cell-grid (:cell-grid world-map)
        radius 0.8
        circle {:position position :radius radius}]
    (draw-circle c position radius (color/rgb 1 0 0 0.5))
    (doseq [[x y] (map #(:position @%)
                       (cell-grid/circle->touched-cells cell-grid circle))]
      (draw-rectangle c x y 1 1 (color/rgb 1 0 0 0.5)))
    (let [{[x y] :left-bottom :keys [width height]} (gdl.math.geom/circle->outer-rectangle circle)]
      (draw-rectangle c x y width height (color/rgb 0 0 1 1)))))

(defn- tile-debug [{:keys [world-camera
                           world-viewport-width
                           world-viewport-height
                           context/world-map] :as c}]
  (let [cell-grid (:cell-grid world-map)
        [left-x right-x bottom-y top-y] (camera/frustum world-camera)]
    (draw-grid c (int left-x) (int bottom-y)
               (inc (int world-viewport-width))
               (+ 2 (int world-viewport-height))
               1 1 (color/rgb 0.5 0.5 0.5 0.5))
    (doseq [[x y] (camera/visible-tiles world-camera)
            :let [cell (get cell-grid [x y])
                  faction :good
                  {:keys [distance entity]} (get-in @cell [faction])]
            :when distance]
      #_(draw-rectangle c (+ x 0.1) (+ y 0.1) 0.8 0.8
                        (if blocked?
                          Color/RED
                          Color/GREEN))
      (let [ratio (/ (int (/ distance 10)) 15)]
        (draw-filled-rectangle c x y 1 1
                               (color/rgb ratio (- 1 ratio) ratio 0.6)))
      #_(@#'g/draw-string x y (str distance) 1)
      #_(when (:monster @cell)
          (@#'g/draw-string x y (str (:id @(:monster @cell))) 1)))))

(extend-type gdl.protocols.Context
  game.protocols/DebugRenderer
  (render-debug-before-entities [c]
    #_(tile-debug c))

  (render-debug-after-entities [{:keys [world-mouse-position
                                        context/world-map]
                                 :as c}]
    #_(geom-test c)
    ; highlight current mouseover-tile
    #_(let [[x y] (mapv int world-mouse-position)
            cell-grid (:cell-grid world-map)
            cell (get cell-grid [x y])]
        (draw-rectangle c x y 1 1 (color/rgb 0 1 0 0.5))
        #_(g/render-readable-text x y {:shift false}
                                  [color/white
                                   (str [x y])
                                   color/gray
                                   (pr-str (:potential-field @cell))])) ))
