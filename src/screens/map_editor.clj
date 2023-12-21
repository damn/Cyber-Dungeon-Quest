(ns screens.map-editor
  (:require [clojure.edn :as edn]
            [gdl.context :refer [key-pressed? key-just-pressed?]]
            [gdl.input.keys :as input.keys]
            gdl.screen
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.context :refer [draw-filled-rectangle draw-filled-circle draw-grid render-world-view ->text-button]]
            gdl.disposable
            [gdl.maps.tiled :as tiled]
            [gdl.scene2d.ui :as ui]
            [app.state :refer [change-screen!]]
            [game.context :refer [all-properties]]
            [mapgen.movement-property :refer (movement-property movement-properties)]
            [mapgen.module-gen :as module-gen])
  (:import com.badlogic.gdx.graphics.OrthographicCamera
           com.badlogic.gdx.maps.tiled.TiledMap
           com.badlogic.gdx.scenes.scene2d.ui.TextField))

(def ^:private current-tiled-map (atom nil))
(def ^:private current-area-level-grid (atom nil))

(defn- center-world-camera [world-camera]
  (camera/set-position! world-camera
                        [(/ (tiled/width  @current-tiled-map) 2)
                         (/ (tiled/height @current-tiled-map) 2)]))

; TODO also highlight current mouseover tile !
(defn- gui-render []
  #_(g/render-readable-text 0 60 {:shift true}
                          (let [tile (->tile (world/mouse-position))
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
; TODO move to gdl
(defn adjust-zoom [^OrthographicCamera camera by] ; TODO minimap also not necessary to @var
  (set! (.zoom camera) (+ (.zoom camera) by))
  (.update camera))

; TODO movement-speed scales with zoom value for big maps useful
(def ^:private camera-movement-speed 1)
(def ^:private zoom-speed 0.05)

(def ^:private show-movement-properties (atom false))
(def ^:private show-grid-lines (atom false))

; TODO textfield takes control !
(defn- camera-controls [context camera]
  (if (key-pressed? context input.keys/plus)  (adjust-zoom camera    zoom-speed)) ; TODO only pass + / -
  (if (key-pressed? context input.keys/minus) (adjust-zoom camera (- zoom-speed)))
  (let [apply-position (fn [idx f]
                         (camera/set-position! camera
                                               (update (camera/position camera)
                                                       idx
                                                       #(f % camera-movement-speed))))]
    (if (key-pressed? context input.keys/left)  (apply-position 0 -))
    (if (key-pressed? context input.keys/right) (apply-position 0 +))
    (if (key-pressed? context input.keys/up)    (apply-position 1 +))
    (if (key-pressed? context input.keys/down)  (apply-position 1 -))))

(def ^:private current-start-positions (atom nil))

(def ^:private show-area-level-colors true)

(defn- render-on-map [{:keys [world-camera] :as c}]
  (let [visible-tiles (camera/visible-tiles world-camera)]
    (when show-area-level-colors
      (if @current-start-positions
        (doseq [[x y] visible-tiles
                :when (@current-start-positions [x y])]
          (draw-filled-rectangle c x y 1 1 [0 0 1 0.5])))
      (doseq [[x y] visible-tiles
              :let [movement-property (movement-property @current-tiled-map [x y])]]
        (when (= :all movement-property)
          (let [level (get @current-area-level-grid [x y])]
            (if (number? level)
              (draw-filled-rectangle c x y 1 1
                                     (if (= level 0)
                                       nil;[0 0 1 0.5]
                                       [(/ level 9)
                                        (- 1 (/ level 9))
                                        0
                                        0.5])))))))

    ; TODO move down to other doseq and make button

    (when @show-movement-properties
      (doseq [[x y] visible-tiles
              :let [movement-property (movement-property @current-tiled-map [x y])]]
        (draw-filled-circle c [(+ x 0.5) (+ y 0.5)]
                            0.08
                            color/black)
        (draw-filled-circle c [(+ x 0.5) (+ y 0.5)]
                            0.05
                            (case movement-property
                              "all"   color/green
                              "air"   color/orange
                              "none"  color/red)))))

  (when @show-grid-lines
    (draw-grid c 0 0
               (tiled/width  @current-tiled-map)
               (tiled/height @current-tiled-map)
               1 1
               [1 1 1 0.5])))

(defn- generate [{:keys [world-camera] :as context} properties]
  (let [{:keys [tiled-map
                area-level-grid
                start-positions]} (module-gen/generate (assoc properties
                                                              :creature-properties (all-properties context :creature)))]
    (.dispose ^TiledMap @current-tiled-map)
    (reset! current-tiled-map tiled-map)
    (reset! current-area-level-grid area-level-grid)
    (reset! current-start-positions (set start-positions))
    (center-world-camera world-camera)))

; for each key k and value v  define
; -> form
; and from table how to get the value

(defn edit-form [[k v]]
  (doto (ui/text-field (str v))
    (.setName (str k))))

(defn form-value [^com.badlogic.gdx.scenes.scene2d.ui.Table forms-table k]
  (.getText ^com.kotcrab.vis.ui.widget.VisTextField (.findActor forms-table (str k))))
; => or reuse exactly entity-editor window

; TODO any key typed and not saved -> show 'unsaved' icon
; save => show saved icon.
; TODO validation/schema (malli/clojure.spec)
; TODO see common stuff w. entity-editor/screen.
(defn- edn-edit-form [context edn-data-file]
  (let [properties (edn/read-string (slurp edn-data-file))
        table (ui/table)
        get-properties #(into {}
                              (for [k (keys properties)]
                                [k (edn/read-string (form-value table k))]))]
    (.colspan (.add table (ui/label edn-data-file)) 2)
    (.row table)
    (doseq [[k v] properties]
      (.add table (ui/label (name k)))
      (.add table ^com.badlogic.gdx.scenes.scene2d.Actor (edit-form [k v]))
      (.row table))
    (.colspan (.add table (->text-button context
                                         (str "Save to file")
                                         (fn [_context]
                                           (spit edn-data-file
                                                 (with-out-str
                                                  (clojure.pprint/pprint
                                                   (get-properties)))))))
              2)
    [table get-properties]))

(defrecord SubScreen []
  gdl.disposable/Disposable
  (dispose [_]
    (.dispose ^TiledMap @current-tiled-map))
  gdl.screen/Screen
  (show [_ {:keys [world-camera]}]
    (center-world-camera world-camera))
  (hide [_ _ctx])
  (render [_ context]
    (tiled/render-map context
                      @current-tiled-map
                      (constantly color/white)) ; TODO colorsetter optional.
    (render-world-view context render-on-map))
  (tick [_ {:keys [world-camera] :as context} delta]
    (if (key-just-pressed? context input.keys/l)
      (swap! show-grid-lines not))
    (if (key-just-pressed? context input.keys/m)
      (swap! show-movement-properties not))
    (camera-controls context world-camera)
    (when (key-just-pressed? context input.keys/escape)
      (change-screen! :screens/main-menu))))

(defn screen [context]
  (reset! current-tiled-map (tiled/load-map module-gen/modules-file))
  (let [window (ui/window :title "Properties")
        [form get-properties] (edn-edit-form context "resources/maps/map.edn")] ; TODO move to properties
    (.add window ^com.badlogic.gdx.scenes.scene2d.Actor form)
    (.row window)
    (.add window (->text-button context "Generate" #(generate % (get-properties))))
    (.pack window)
    {:actors [window]
     :sub-screen (->SubScreen)}))



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
