; TODO move to screens/ingame ( also update-ingame)
(ns game.render-ingame
  (:require [gdl.graphics.world :as world]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.color :as color]
            [gdl.tiled :as tiled]
            game.player.status-gui

            [game.utils.lightning :refer [tile-color-setter]]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.data :refer [get-current-map-data]]

            [game.components.render :refer [render-entities*]]

            ; Ingame GUI render fns
            [game.player.status-gui :refer [render-player-hp-mana]]
            [game.utils.msg-to-player :refer [render-message-to-player]]
            ; Ingame GUI render fns
            [game.update-ingame :refer (thrown-error)]
            [game.player.entity :refer (player-entity)]
            [game.maps.contentfields :refer [get-entities-in-active-content-fields]]
            [game.maps.potential-field :as potential-field]))

(defn- geom-test []
  (let [position (world/mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (shape-drawer/circle position radius (color/rgb 1 0 0 0.5))

    (doseq [[x y] (map #(:position @%)
                       (@#'game.maps.cell-grid/circle->touched-cells circle))]
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
  (let [[left-x right-x bottom-y top-y] (world/camera-frustum)]
    (shape-drawer/grid (int left-x)
                       (int bottom-y)
                       (inc (int (world/viewport-width)))
                       (+ 2 (int (world/viewport-height)))
                       1
                       1
                       (color/rgb 0.5 0.5 0.5 0.5))

    (doseq [[x y] (world/visible-tiles)
            :let [cell (game.maps.cell-grid/get-cell [x y])
                  faction :good
                  {:keys [distance entity]} (get-in @cell [faction])]
            :when distance]
      #_(shape-drawer/rectangle (+ x 0.1) (+ y 0.1) 0.8 0.8
                                (if blocked?
                                  color/red
                                  color/green))

      (let [ratio (/ (int (/ distance 10)) 15)]
        (shape-drawer/filled-rectangle x y 1 1
                                       (color/rgb ratio (- 1 ratio) ratio 0.6)))

      #_(@#'g/draw-string x y (str distance) 1)

      #_(when (:monster @cell)
          (@#'g/draw-string x y (str (:id @(:monster @cell))) 1)))))

(defn- render-map-content []
  #_(tile-debug) ; TODO make debug options for everything. (dear-imgui ?)

  (render-entities* (visible-entities*))

  #_(geom-test)


  ; highlight current mouseover-tile
  #_(let [[x y] (mapv int (world/mouse-position))
          cell (game.maps.cell-grid/get-cell [x y])]
      (shape-drawer/rectangle x y 1 1 (color/rgb 0 1 0 0.5))
      (g/render-readable-text x y {:shift false}
                              [color/white
                               (str [x y])
                               color/gray
                               (pr-str (:potential-field @cell))])))

(defn- print-mouse-tile-position []
  (let [[tile-x tile-y] (world/mouse-position)]
    (str (float tile-x) " " (float tile-y))))

; y selbst aufbauen mit font height.
#_(defn- render-debug [x mouseover-body]
  (let [starty 30
        text nil
        lineh (get-text-height text)]

    (render-readable-text 0 0 {} (str (get-fps)))

    (render-readable-text x (+ starty (* lineh 0)) {} (str "mouse" (get-mouse-pos)))

    (when debug/show-contentfield
      (render-readable-text x (+ starty (* lineh 1)) {} (str "player content field:"  (get-player-content-field-idx))))

    (when debug/show-float-mouse-pos
      (render-readable-text x (+ starty (* lineh 3)) {} (str "maus-tile x,y = "  (print-mouse-tile-position))))

    (when debug/show-tile-mouse-pos
      (render-readable-text x (+ starty (* lineh 4)) {} (str "int-tile x,y = "  (mouse-int-tile-pos))))))

; TODO do everything with scene2d
(defn- render-gui []
  (render-player-hp-mana)
  (render-message-to-player)) ; TODO render over gui windows... in extra window @ stage ?? top lvl ?

(defn render-game []
  (tiled/render-map (:tiled-map (get-current-map-data))
                    #'tile-color-setter)
  (world/render render-map-content)
  (gui/render render-gui))
