(ns context.render-debug
  (:require [gdl.context :refer [draw-circle draw-rectangle draw-filled-rectangle draw-grid
                                 world-mouse-position]]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            gdl.math.geom
            [utils.core :refer [->tile]]
            [game.context :refer [world-grid]]
            [game.world.grid :refer [circle->cells]]))

; TODO make check-buttons with debug-window or MENU top screen is good for debug I think

(defn- geom-test [c]
  (let [position (world-mouse-position c)
        grid (world-grid c)
        radius 0.8
        circle {:position position :radius radius}]
    (draw-circle c position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%)
                       (circle->cells grid circle))]
      (draw-rectangle c x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (gdl.math.geom/circle->outer-rectangle circle)]
      (draw-rectangle c x y width height [0 0 1 1]))))

(defn- tile-debug [{:keys [world-camera
                           world-viewport-width
                           world-viewport-height] :as c}]
  (let [grid (world-grid c)
        [left-x right-x bottom-y top-y] (camera/frustum world-camera)]
    (draw-grid c (int left-x) (int bottom-y)
               (inc (int world-viewport-width))
               (+ 2 (int world-viewport-height))
               1 1 [0.5 0.5 0.5 0.5])
    (doseq [[x y] (camera/visible-tiles world-camera)
            :let [cell (get grid [x y])
                  faction :good
                  {:keys [distance entity]} (get-in @cell [faction])]
            :when distance]
      #_(draw-rectangle c (+ x 0.1) (+ y 0.1) 0.8 0.8
                        (if blocked?
                          color/red
                          color/green))
      (let [ratio (/ (int (/ distance 10)) 15)]
        (draw-filled-rectangle c x y 1 1
                               [ratio (- 1 ratio) ratio 0.6]))
      #_(@#'g/draw-string x y (str distance) 1)
      #_(when (:monster @cell)
          (@#'g/draw-string x y (str (:id @(:monster @cell))) 1)))))

(extend-type gdl.context.Context
  game.context/DebugRender
  (debug-render-before-entities [c]
    #_(tile-debug c))

  (debug-render-after-entities [c]
    #_(geom-test c)
    ; highlight current mouseover-tile
    #_(let [[x y] (world-mouse-position c)
          cell (get (world-grid c) (->tile [x y]))]
        (draw-rectangle c x y 1 1 [0 1 0 0.5])
        #_(g/render-readable-text x y {:shift false}
                                  [color/white
                                   (str [x y])
                                   color/gray
                                   (pr-str (:potential-field @cell))]))))
