(ns game.maps.tile-color-setters
  (:require [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [app.state :refer [current-context]]
            [game.context :refer [ray-blocked?]])
  (:import com.badlogic.gdx.graphics.Color))

(defn- explored? [{:keys [context/world-map] :as context} position]
  (get @(:explored-tile-corners world-map)
       position) )

(defn set-explored! [{:keys [context/world-map] :as context} position]
  (swap! (:explored-tile-corners world-map) assoc (mapv int position) true))

(defn minimap-color-setter [color x y]
  (if (explored? @current-context [x y])
    Color/WHITE
    Color/BLACK))

; TODO performance - need to deref current-context at every tile corner !!
; => see with prformance check later
; => need to pass to orthogonaltiledmap bla
; or pass only necessary data structures  (explored grid)

(def ^:private explored-tile-color (color/rgb 0.5 0.5 0.5))

(defn tile-color-setter [_ x y]
  (let [{:keys [world-camera] :as context}  @current-context
        light-position (camera/position world-camera)
        position [x y]
        explored? (explored? context position)
        base-color (if explored?
                     explored-tile-color
                     Color/BLACK)
        blocked? (ray-blocked? context light-position position)]
    (if blocked?
      base-color
      (do
       (when-not explored?
         (set-explored! context position))
       Color/WHITE))))
