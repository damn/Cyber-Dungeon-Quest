(ns game.render-ingame
  (:require [gdl.graphics.world :as world]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.color :as color]
            [gdl.tiled :as tiled]
            [game.utils.lightning :refer [tile-color-setter]]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.data :refer [get-current-map-data]]
            [game.maps.cell-grid :as cell-grid]
            [game.context :as context]
            [game.render :as render]
            [game.player.status-gui :refer [render-player-hp-mana]]
            [game.utils.msg-to-player :refer [render-message-to-player]]
            [game.update-ingame :refer (thrown-error)]
            [game.player.entity :refer (player-entity)]
            [game.maps.contentfields :refer [get-entities-in-active-content-fields]]
            [game.maps.potential-field :as potential-field])
  (:import com.badlogic.gdx.graphics.Color))

(defn- geom-test []
  (let [position (world/mouse-position)
        cell-grid (:cell-grid (get-current-map-data))
        radius 0.8
        circle {:position position :radius radius}]
    (shape-drawer/circle position radius (color/rgb 1 0 0 0.5))
    (doseq [[x y] (map #(:position @%)
                       (cell-grid/circle->touched-cells cell-grid circle))]
      (shape-drawer/rectangle x y 1 1 (color/rgb 1 0 0 0.5)))
    (let [{[x y] :left-bottom :keys [width height]} (gdl.geom/circle->outer-rectangle circle)]
      (shape-drawer/rectangle x y width height (color/rgb 0 0 1 1)))))

(comment
 (count (filter #(:sleeping @%) (get-entities-in-active-content-fields)))
 )

(defn- visible-entities* []
  (->> (get-entities-in-active-content-fields)
       (map deref)
       (filter #(in-line-of-sight? @player-entity %))))

(defn- tile-debug []
  (let [cell-grid (:cell-grid (get-current-map-data))
        [left-x right-x bottom-y top-y] (world/camera-frustum)]
    (shape-drawer/grid (int left-x)
                       (int bottom-y)
                       (inc (int (world/viewport-width)))
                       (+ 2 (int (world/viewport-height)))
                       1
                       1
                       (color/rgb 0.5 0.5 0.5 0.5))
    (doseq [[x y] (world/visible-tiles)
            :let [cell (get cell-grid [x y])
                  faction :good
                  {:keys [distance entity]} (get-in @cell [faction])]
            :when distance]
      #_(shape-drawer/rectangle (+ x 0.1) (+ y 0.1) 0.8 0.8
                                (if blocked?
                                  Color/RED
                                  Color/GREEN))
      (let [ratio (/ (int (/ distance 10)) 15)]
        (shape-drawer/filled-rectangle x y 1 1
                                       (color/rgb ratio (- 1 ratio) ratio 0.6)))
      #_(@#'g/draw-string x y (str distance) 1)
      #_(when (:monster @cell)
          (@#'g/draw-string x y (str (:id @(:monster @cell))) 1)))))

(defn- render-map-content [unit-scale]
  #_(tile-debug)
  (render/render-entities* (context/get-context unit-scale)
                           (visible-entities*))
  #_(geom-test)
  ; highlight current mouseover-tile
  #_(let [[x y] (mapv int (world/mouse-position))
        cell-grid (:cell-grid (get-current-map-data))
        cell (get cell-grid [x y])]
    (shape-drawer/rectangle x y 1 1 (color/rgb 0 1 0 0.5))
    #_(g/render-readable-text x y {:shift false}
                            [color/white
                             (str [x y])
                             color/gray
                             (pr-str (:potential-field @cell))])))

(defn- print-mouse-tile-position []
  (let [[tile-x tile-y] (world/mouse-position)]
    (str (float tile-x) " " (float tile-y))))

; TODO use scene2d
(defn- render-gui [unit-scale]
  (render-player-hp-mana (context/get-context unit-scale))
  (render-message-to-player))

(defn render-game [batch]
  (tiled/render-map batch
                    (:tiled-map (get-current-map-data))
                    #'tile-color-setter)
  (world/render batch render-map-content)
  (gui/render batch render-gui))
