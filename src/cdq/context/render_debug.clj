(ns cdq.context.render-debug
  (:require [gdl.context :refer [draw-circle draw-rectangle draw-filled-rectangle draw-grid
                                 world-mouse-position]]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            gdl.math.geom
            [utils.core :refer [->tile]]
            [cdq.context :refer [world-grid]]
            [cdq.world.grid :refer [circle->cells]]))

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

(def ^:private tile-grid? false)
(def ^:private potential-field-colors? false)
(def ^:private cell-entities? false)
(def ^:private cell-occupied? false)

(require '[cdq.context.potential-fields :as potential-field])

(defn- tile-debug [{:keys [world-camera
                           world-viewport-width
                           world-viewport-height] :as ctx}]
  (let [grid (world-grid ctx)
        [left-x right-x bottom-y top-y] (camera/frustum world-camera)]

    (when tile-grid?
      (draw-grid ctx (int left-x) (int bottom-y)
                 (inc (int world-viewport-width))
                 (+ 2 (int world-viewport-height))
                 1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles world-camera)
            :let [cell (grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (draw-filled-rectangle ctx x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (draw-filled-rectangle ctx x y 1 1 [0 0 1 0.6]))

      #_(draw-rectangle ctx (+ x 0.1) (+ y 0.1) 0.8 0.8
                        (if blocked?
                          color/red
                          color/green))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance entity]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (@#'potential-field/factions-iterations faction))]
              (draw-filled-rectangle ctx x y 1 1 [ratio (- 1 ratio) ratio 0.6])))))
      #_(@#'g/draw-string x y (str distance) 1)
      #_(when (:monster @cell)
          (@#'g/draw-string x y (str (:id @(:monster @cell))) 1)))))


(comment
 (let [ctx @gdl.app/current-context
       [x y] (->tile (world-mouse-position ctx))
       cell* @((world-grid ctx) [x y])]
   (clojure.pprint/pprint
    cell*)

   ; TODO occupied nil !
   ))

(def ^:private highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [ctx]
  (when highlight-blocked-cell?
    (let [[x y] (->tile (world-mouse-position ctx))
          cell (get (world-grid ctx) [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (draw-rectangle ctx x y 1 1
                        (case (:movement @cell)
                          :air  [1 1 0 0.5]
                          :none [1 0 0 0.5]))))))

(extend-type gdl.context.Context
  cdq.context/DebugRender
  (debug-render-before-entities [ctx]
    (tile-debug ctx))

  (debug-render-after-entities [ctx]
    #_(geom-test ctx)
    (highlight-mouseover-tile ctx)))
