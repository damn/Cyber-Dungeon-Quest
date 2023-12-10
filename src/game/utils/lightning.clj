(ns game.utils.lightning
  (:require [gdl.graphics.color :as color]
            [gdl.graphics.world :as world]
            [game.maps.data :refer (get-current-map-data)]
            [game.maps.cell-grid :as cell-grid]))

#_(defprotocol LightSource
  (color-at [_ distance]))

#_(defn- linear-falloff [intensity distance radius]
  (let [i    (float intensity)
        dist (float distance)
        r    (float radius)]
    (* i
       (- 1
          (/ dist r)))))

#_(defn- get-intensity [{:keys [radius intensity falloff]} distance]
  (cond
    (> distance radius) 0
    (not falloff)  (linear-falloff intensity distance radius)
    :else (let [max-intensity-radius (- radius falloff)]
            (cond
              (< distance max-intensity-radius) intensity
              :else
              (linear-falloff intensity (- distance max-intensity-radius) falloff)))))

#_(import 'com.badlogic.gdx.graphics.Color)

; TODO move into engine this code
#_(defn- multiply-color [^Color color ^Color color2]
  (.mul (.cpy color) color2))

; TODO does not take into account width/height of an entity! maybe for square ones (+ radius half-w)
#_(defrecord RadiusLightSource [color intensity radius falloff]
  LightSource
  (color-at [this distance]
    (if (<= distance radius)
      (multiply-color color
                         (let [i (get-intensity this distance)]
                           (color/rgb i i i i))))))

#_(def player-light-source
  (map->RadiusLightSource {:color color/white
                           :intensity 1
                           :falloff 0 ; TODO rename max-intensity-radius
                           :radius 17
                           ; => max-intensity-radius = (- radius falloff)
                           }))

; IMPROVEMENT:
; * cache ray blocked (max 1000 entries?)
; * round ray blocked calls to 0.3 or 0.5 tiles before caching
; * cache intensity call
; * make lightmap & cache for position with posi changed epsilon 1/3 tile or so 6 pxs.

(def ^:private explored-tile-color (color/rgb 0.5 0.5 0.5))

(defn- explored? [position]
  (get @(:explored-tile-corners (get-current-map-data))
       position) )

(defn set-explored! [position]
  (swap! (:explored-tile-corners (get-current-map-data)) assoc (mapv int position) true))

(defn minimap-color-setter [color x y]
  (if (explored? [x y])
    color/white
    color/black))

(def ^:private lightmap (atom {}))
(def ^:private cached-position (atom nil))


; TODO we are not using the batch-color yet as argument so not multiplying


; 1720 tile color setter calls & only ~500 lightmap positions
; but why do I not see a FPS difference ?!

;  new
; "Elapsed time: 170.404936 msecs"
; old
; "Elapsed time: 96.788767 msecs"
; => new version is SLOWER than old version ( every time distance check ...)
; => do lightmap only once check if old lightmap is expired (> distance threshold)
(comment
 (println "\nnew")
 (time (dotimes [_ 100]
         (tiled/render-map batch
                           (:tiled-map (get-current-map-data))
                           #'tile-color-setter-new)))
 (println "old")
 (time (dotimes [_ 100]
         (tiled/render-map batch
                           (:tiled-map (get-current-map-data))
                           #'tile-color-setter-old))))
#_(defn tile-color-setter-new [_ x y]
  (let [light-position (world/camera-position)
        position [x y]
        explored? (explored? position)
        base-color (if explored?
                     explored-tile-color
                     color/black)
        _ (when (or (not @cached-position)
                    (> (gdl.vector/distance @cached-position
                                                    light-position)
                       0.5))
            (do
             (reset! lightmap {})
             (reset! cached-position light-position)))
        blocked? (if (contains? @lightmap position)
                   true
                   (let [blocked? (cell-grid/ray-blocked? light-position position)]
                     (when blocked?
                       (swap! lightmap assoc position blocked?))
                     blocked?))]
    (if blocked?
      base-color
      (do
       (when-not explored?
         (set-explored! position))
       color/white))))

(defn tile-color-setter [_ x y]
  (let [light-position (world/camera-position)
        position [x y]
        explored? (explored? position)
        base-color (if explored?
                     explored-tile-color
                     color/black)
        blocked? (cell-grid/ray-blocked? (get-current-map-data) light-position position)]
    (if blocked?
      base-color
      (do
       (when-not explored?
         (set-explored! position))
       color/white))))

#_(let [distance (v/distance light-position position)
        lighted-color (color-at player-light-source distance)]
    (if lighted-color
      (do
       (swap! (:explored-tile-corners (get-current-map-data)) assoc (mapv int position) true)
       (.add lighted-color base-color))
      base-color))
