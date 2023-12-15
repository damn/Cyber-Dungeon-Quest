(ns game.render
  (:require [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.maps.tiled :as tiled]
            [utils.core :refer [define-order sort-by-order]]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.cell-grid :as cell-grid]
            [game.entity :as entity]
            [game.ui.hp-mana-bars :refer [render-player-hp-mana]]
            [game.utils.lightning :refer [tile-color-setter]]
            [game.maps.contentfields :refer [get-entities-in-active-content-fields]])
  (:import com.badlogic.gdx.graphics.Color))

; TODO render-order make vars so on compile time checked ?

(def render-on-map-order
  (define-order
    [:on-ground ; items
     :ground    ; creatures, player
     :flying    ; flying creatures
     :effect])) ; projectiles, nova
                ; :info ( TODO only 1 -> make render-debug ? )

; TODO also add it to thrown-error ! and pause the game ....
; sometimes it only draws for 1 frame and might miss the error when not looking @ console.
#_(defn- handle-throwable [t a {:keys [id position]}]
  (let [info (str "Render error [" a " | TODO RENDERFN | id:" id "]")
        [x y] position]
    (println info)
    (println "Throwable:\n"t)
    ; red color ? (str "[RED]" ..)
    ; center-x -> alignment :center ? check opts. left/right only ?
    ; background ? -> dont have, do I need it ? maybe. with outline/border ?
    ; default font or game font ?
    ; or just highlight that entity somehow
    ; or ignore @ normal game , at debug highlight and stop game.
    (draw/text drawer {:x x,:y y,:text (str "[RED]" info)})))

; if lightning => pass render-on-map argument 'colorsetter' by default
; on all render-systems , has to be handled & everything has to have body then?
; so all have line of sight check & colors applied as of position ?
(defn- render-entity* [system drawer context entity*]
  (doseq [component entity*]
    (try
     (system component drawer context entity*)
     (catch Throwable t
       (println "Render error for:" [component (:id entity*) system])
       (throw t)
       ; TODO I want to get multimethod
       ))))
; TODO throw/catch renderfn missing & pass body ?
; TODO position needed? entity* has it in keys, we might use bottom-left

(defn- render-entities* [drawer context entities*]
  (doseq [[_ entities*] (sort-by-order (group-by :z-order entities*)
                                       first
                                       render-on-map-order)
          system [entity/render-below
                  entity/render-default
                  entity/render-above
                  entity/render-info]
          entity* entities*]
    (render-entity* system drawer context entity*))
  (doseq [entity* entities*]
    (render-entity* entity/render-debug drawer context entity*)))



(defn- geom-test [drawer {:keys [world-mouse-position context/world-map]}]
  (let [position world-mouse-position
        cell-grid (:cell-grid world-map)
        radius 0.8
        circle {:position position :radius radius}]
    (draw/circle drawer position radius (color/rgb 1 0 0 0.5))
    (doseq [[x y] (map #(:position @%)
                       (cell-grid/circle->touched-cells cell-grid circle))]
      (draw/rectangle drawer x y 1 1 (color/rgb 1 0 0 0.5)))
    (let [{[x y] :left-bottom :keys [width height]} (gdl.math.geom/circle->outer-rectangle circle)]
      (draw/rectangle drawer x y width height (color/rgb 0 0 1 1)))))

(comment
 (count (filter #(:sleeping @%) (get-entities-in-active-content-fields)))
 )

(defn- visible-entities* [{:keys [context/player-entity] :as context}]
  (->> (get-entities-in-active-content-fields context)
       (map deref)
       (filter #(in-line-of-sight? @player-entity % context))))

(defn- tile-debug [drawer {:keys [world-camera world-viewport-width world-viewport-height
                                  context/world-map]}]
  (let [cell-grid (:cell-grid world-map)
        [left-x right-x bottom-y top-y] (camera/frustum world-camera)]
    (draw/grid drawer (int left-x)
                       (int bottom-y)
                       (inc (int world-viewport-width))
                       (+ 2 (int world-viewport-height))
                       1
                       1
                       (color/rgb 0.5 0.5 0.5 0.5))
    (doseq [[x y] (camera/visible-tiles world-camera)
            :let [cell (get cell-grid [x y])
                  faction :good
                  {:keys [distance entity]} (get-in @cell [faction])]
            :when distance]
      #_(draw/rectangle drawer (+ x 0.1) (+ y 0.1) 0.8 0.8
                        (if blocked?
                          Color/RED
                          Color/GREEN))
      (let [ratio (/ (int (/ distance 10)) 15)]
        (draw/filled-rectangle drawer x y 1 1
                               (color/rgb ratio (- 1 ratio) ratio 0.6)))
      #_(@#'g/draw-string x y (str distance) 1)
      #_(when (:monster @cell)
          (@#'g/draw-string x y (str (:id @(:monster @cell))) 1)))))

(defn- render-map-content [drawer {:keys [world-mouse-position
                                         context/world-map]
                                  :as context}]
  #_(tile-debug drawer context)

  (render-entities* drawer
                    context
                    (visible-entities* context))

  #_(geom-test drawer context)

  ; highlight current mouseover-tile
  #_(let [[x y] (mapv int world-mouse-position)
        cell-grid (:cell-grid world-map)
        cell (get cell-grid [x y])]
    (draw/rectangle drawer x y 1 1 (color/rgb 0 1 0 0.5))
    #_(g/render-readable-text x y {:shift false}
                            [color/white
                             (str [x y])
                             color/gray
                             (pr-str (:potential-field @cell))])))

(defn- render-gui [drawer context]
  (render-player-hp-mana drawer context))

(defn render-game [{:keys [context/world-map] :as context}]
  (tiled/render-map context
                    (:tiled-map world-map)
                    #'tile-color-setter)
  (app/render-with context
                   :world
                   #(game.render/render-map-content % context))
  (app/render-with context
                   :gui
                   #(game.render/render-gui % context)))
