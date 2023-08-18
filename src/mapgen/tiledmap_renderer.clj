(nsx mapgen.tiledmap-renderer
  (:require [clojure.edn :as edn]
            game.maps.impl
            [mapgen.movement-property :refer (movement-property movement-properties)]
            [mapgen.module-gen :as module-gen]))

(def ^:private current-tiled-map (atom nil))
(def ^:private current-area-level-grid (atom nil))

; TODO also zoom so to see all visible tiles
; -> useful for minimap later
(defn- center-world-camera []
  (world/set-camera-position! [(/ (tiled/width  @current-tiled-map) 2)
                               (/ (tiled/height @current-tiled-map) 2)]))

; TODO also highlight current mouseover tile !
(defn- gui-render []
  #_(g/render-readable-text 0 60 {:shift true}
                          (let [tile (mapv int (world/mouse-position))
                                tiled-map @current-tiled-map]
                            [(str "Area coords:" (mapv (comp int /) (world/mouse-position) [32 20]))
                             (str "Map coords:" (world/mouse-position))
                             (str "Tile coords:" tile)
                             ;(str "Visible tiles: " (world/camera-frustum))
                             (when @current-area-level-grid
                               (let [level (get @current-area-level-grid tile)]
                                 (when (number? level)
                                   (str "Area level:" level))))
                             #_(when tiled-map
                                 (str "Movement properties: " (apply vector (movement-properties tiled-map tile))))
                             #_(when tiled-map
                                 (str "Movement property: " (movement-property tiled-map tile)))])))

; TODO try camera rotate also
; TODO clamp movement / zoom / zoom from max zoom rate (see whole map , to see 1 tile, percentage based )
; maybe even a slider?
; https://libgdx.com/wiki/graphics/2d/orthographic-camera
(defn adjust-zoom [by]
  (let [camera world/camera] ; TODO minimap also not necessary to @var
    (set! (.zoom camera) (+ (.zoom camera) by))
    (.update camera)))

; TODO movement-speed scales with zoom value for big maps useful
(def ^:private camera-movement-speed 1)
(def ^:private zoom-speed 0.05)

(def ^:private show-movement-properties (atom false))
(def ^:private show-grid-lines (atom false))

; TODO textfield takes control !
(defn- camera-controls []
  (if (input/is-key-down? :PLUS)  (adjust-zoom    zoom-speed)) ; TODO only pass + / -
  (if (input/is-key-down? :MINUS) (adjust-zoom (- zoom-speed)))
  (let [apply-position (fn [idx f]
                         (world/set-camera-position!
                          (update (world/camera-position)
                                  idx
                                  #(f % camera-movement-speed))))]
    (if (input/is-key-down? :LEFT)  (apply-position 0 -))
    (if (input/is-key-down? :RIGHT) (apply-position 0 +))
    (if (input/is-key-down? :UP)    (apply-position 1 +))
    (if (input/is-key-down? :DOWN)  (apply-position 1 -))))

(def ^:private current-start-positions (atom nil))

(def ^:private show-area-level-colors true)

(defn- render-on-map []
  (let [visible-tiles (world/visible-tiles)]
    (when show-area-level-colors
      (if @current-start-positions
        (doseq [[x y] visible-tiles
                :when (@current-start-positions [x y])]
          (shape-drawer/filled-rectangle x y 1 1 (color/rgb 0 0 1 0.5))))
      (doseq [[x y] visible-tiles
              :let [movement-property (movement-property @current-tiled-map [x y])]]
        (when (= :all movement-property)
          (let [level (get @current-area-level-grid [x y])]
            (if (number? level)
              (shape-drawer/filled-rectangle x y 1 1
                                             (if (= level 0)
                                               nil;(color/rgb 0 0 1 0.5)
                                               (color/rgb (/ level 9)
                                                          (- 1 (/ level 9))
                                                          0
                                                          0.5))))))))

    ; TODO move down to other doseq and make button

    (when @show-movement-properties
      (doseq [[x y] visible-tiles
              :let [movement-property (movement-property @current-tiled-map [x y])]]
        (shape-drawer/filled-circle [(+ x 0.5) (+ y 0.5)]
                                    0.08
                                    color/black)
        (shape-drawer/filled-circle [(+ x 0.5) (+ y 0.5)]
                                    0.05
                                    (case movement-property
                                      "all"   color/green
                                      "air"   color/orange
                                      "none"  color/red)))))

  (when @show-grid-lines
    (shape-drawer/grid 0 0
                       (tiled/width  @current-tiled-map)
                       (tiled/height @current-tiled-map)
                       1 1
                       (color/rgb 1 1 1 0.5))))

(defn- generate [properties]
  (let [{:keys [tiled-map
                area-level-grid
                start-positions]} (module-gen/generate properties)]
    (.dispose @current-tiled-map)
    (reset! current-tiled-map tiled-map)
    (reset! current-area-level-grid area-level-grid)
    (reset! current-start-positions (set start-positions))
    (center-world-camera)))

; TODO any key typed and not saved -> show 'unsaved' icon
; save => show saved icon.
; TODO validation/schema (malli/clojure.spec)
(defn- edn-edit-form [edn-data-file]
  (let [properties (edn/read-string (slurp edn-data-file))
        table (ui/table)
        get-properties #(into {}
                              (for [k (keys properties)]
                                [k (edn/read-string (.getText (.findActor table (str k))))]))]
    (.colspan (.add table (ui/label edn-data-file)) 2)
    (.row table)
    (doseq [[k v] properties]
      (.add table (ui/label (name k)))
      (.add table (doto (ui/text-field (str v))
                    (.setName (str k))))
      (.row table))
    (.colspan (.add table (ui/text-button (str "Save to file")
                                          #(spit edn-data-file
                                                 (with-out-str
                                                  (clojure.pprint/pprint
                                                   (get-properties))))))
              2)
    [table get-properties]))

(defn- create-stage []
  (let [stage (ui/stage)
        window (ui/window "Properties")
        [form get-properties] (edn-edit-form game.maps.impl/map-data-file) ]
    (.addActor stage window)
    (.add window form)
    (.row window)
    (.add window (ui/text-button "Generate" #(generate (get-properties))))
    (.pack window)
    stage))

(defmodule stage
  (lc/create [_]
    (create-stage))
  (lc/dispose [_]
    (.dispose stage)
    (.dispose @current-tiled-map))
  (lc/show [_]
    (input/set-processor stage)
    (reset! current-tiled-map (tiled/load-map module-gen/modules-file))
    (center-world-camera))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_]
    (when @current-tiled-map
      (tiled/render-map @current-tiled-map
                        (fn [color x y] color/white))
      (world/render render-on-map))
    (gui/render #(ui/draw-stage stage)))
  (lc/tick [_ delta]
    (ui/update-stage stage delta)
    (if (input/is-key-pressed? :L)
      (swap! show-grid-lines not))
    (if (input/is-key-pressed? :M)
      (swap! show-movement-properties not))
    (camera-controls)))

; TODO remove key controls , add checkboxes
; TODO fix mouse movement etc
; TODO back to main menu
; TODO confirmation saved/edited/??
; TODO fix zoom touchpad / show whole map


; TODO  bug zoomed out and mouse under the last down tiles
; still shows 0 tiles
; left also and down is one more row which gets map-coords of 0
; paint the background color different than the viewport-black-stripes
; so no misunderstanding

; TODO map-coords are clamped ? thats why showing 0 under and left of the map?
; make more explicit clamped-map-coords ?


; TODO
; leftest two tiles are 0 coordinate x
; and rightest is 16, not possible -> check clamping
; depends on screen resize or something, changes,
; maybe update viewport not called on resize sometimes

; (flood-fill grid start :opt :steps :opt-def :label nil)
; [grid labeled labeled-ordered]



; for distance -> use this
; for multiple possible paths ( level 3 enemies next to starting area ... etc.)
; do not use all positions for making the next step but only part of it
; (also need to test -> 0.5, 0.7 , 0.9 ??
; for now with backtracking when 2 directions then you get very high level very fast and
; have to go other direction, meeting lower level enemies ...



#_(defn- color-flood-fill-heatmap [grid start]
  (let [[_ _ labeled-ordered] (flood-fill grid start)
        ]

    @agrid))
; TODO what label (assoc -ks )
; TODO what is ground check = get grid posi :ground ?
; -> then place entities -> how / rand chance, increasing, can make more than 3 lvls
; where increasing also the chance and also the chance which level enemy
; -> can make 10+ levels / 99 levels (full of enemies)

; -> need to place enemies & render them -> & read them
; place random lvl1 enemy ? get lvl1 enemies ? creature data move out of 'game' ?! !!


; -> use creature TSX tileset and add to tilemap as layer also
; there is all information names - pictures
; -> import tileset to manually created TiledMap


; -> need to get creatures with lvl X
; and also get their image (use tsx ?!)

; start with very slow opponents, very low hp, damage
; and increase slowly as of level
; so there can be an enemy next to you at start

; also can use heatmap lvl for calculating lvl ? but what about max-lvl ?
; can be a function of dist-to-player itself
; but then backtracking is stupid
; -> make linear ?!



;;;;;;


;;; set creature tiles as of area-levels
;;; use creature tileset & map-layer 'creatures'
;;; read @ game.maps.load
;;; -> can also manually add creatures in modules then???



;;; -> spawn-groups, what can/should spawn together ?
; no goblins & elves ?
; define groups ?
; define group-type & weight and define groups through that
; group-type 'elves' -> all with that type , and weight is added also
; use existing spawn group code, do not throw away ?

; spawn spaces ?
