(ns screens.map-editor
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [gdl.app :refer [change-screen!]]
            [gdl.context :refer [key-pressed? key-just-pressed? ->text-field ->table
                                 ->label ->window world-mouse-position ->actor ->tiled-map render-tiled-map
                                 draw-filled-rectangle draw-filled-circle draw-grid render-world-view ->text-button
                                 current-screen draw-rectangle]]
            [gdl.disposable :refer [dispose]]
            [gdl.input.keys :as input.keys]
            gdl.screen
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.maps.tiled :as tiled]
            [gdl.scene2d.actor :refer [set-position!]]
            [gdl.scene2d.group :refer [add-actor!]]
            [gdl.scene2d.ui.widget-group :refer [pack!]]
            [gdl.scene2d.ui.label :refer [set-text!]]
            [utils.core :refer [->tile]]
            [cdq.context :refer [all-properties]]
            [mapgen.movement-property :refer (movement-property movement-properties)]
            [mapgen.module-gen :as module-gen])
  (:import com.badlogic.gdx.scenes.scene2d.ui.TextField))

(defn- show-whole-map! [camera tiled-map]
  (camera/set-position! camera
                        [(/ (tiled/width  tiled-map) 2)
                         (/ (tiled/height tiled-map) 2)])
  (camera/set-zoom! camera
                    (camera/calculate-zoom camera
                                           :left [0 0]
                                           :top [0 (tiled/height tiled-map)]
                                           :right [(tiled/width tiled-map) 0]
                                           :bottom [0 0])))

(defn- current-data [ctx]
  (-> ctx
      current-screen
      :sub-screen
      :current-data))

(def ^:private infotext
  "L: grid lines
M: movement properties
zoom: shift-left,minus
ESCAPE: leave
direction keys: move")

(defn- debug-infos [ctx]
  (let [tile (->tile (world-mouse-position ctx))
        {:keys [tiled-map
                area-level-grid]} @(current-data ctx)]
    (->> [infotext
          "\n"
          (str "Tile coords:" tile)
          (when-not area-level-grid
            (str "Module index:" (mapv (comp int /)
                                       (world-mouse-position ctx)
                                       [mapgen.module-gen/module-width
                                        mapgen.module-gen/module-height])))
          (when area-level-grid
            (str "Creature id: " (tiled/property-value tiled-map :creatures tile :id)))
          (when area-level-grid
            (let [level (get area-level-grid tile)]
              (when (number? level)
                (str "Area level:" level))))
          (str "Movement properties: \n" (apply vector (movement-properties tiled-map tile))
               "\nResult: " (movement-property tiled-map tile))]
         (remove nil?)
         (str/join "\n"))))

; same as debug-window
(defn- ->info-window [ctx]
  (let [label (->label ctx "")
        window (->window ctx {:title "Info" :rows [[label]]})]
    (add-actor! window (->actor ctx {:act #(do
                                            (set-text! label (debug-infos %))
                                            (pack! window))}))
    ;(set-position! window 500 500)
    (.centerWindow window)
    window))

(defn- adjust-zoom [camera by]
  (camera/set-zoom! camera
                    (max 0.1 (+ (camera/zoom camera) by))))

; TODO movement-speed scales with zoom value for big maps useful
(def ^:private camera-movement-speed 1)
(def ^:private zoom-speed 0.1)

; TODO textfield takes control !
(defn- camera-controls [context camera]
  (when (key-pressed? context input.keys/shift-left)
    ; TODO PLUS symbol shift & = symbol on keyboard not registered
    (adjust-zoom camera    zoom-speed)) ; TODO only pass + / -
  (when (key-pressed? context input.keys/minus)
    (adjust-zoom camera (- zoom-speed)))
  (let [apply-position (fn [idx f]
                         (camera/set-position! camera
                                               (update (camera/position camera)
                                                       idx
                                                       #(f % camera-movement-speed))))]
    (if (key-pressed? context input.keys/left)  (apply-position 0 -))
    (if (key-pressed? context input.keys/right) (apply-position 0 +))
    (if (key-pressed? context input.keys/up)    (apply-position 1 +))
    (if (key-pressed? context input.keys/down)  (apply-position 1 -))))

#_(def ^:private show-area-level-colors true)
; TODO unused
; TODO also draw numbers of area levels big as module size...

(defn- render-on-map [{:keys [world-camera] :as c}]
  (let [{:keys [tiled-map
                area-level-grid
                start-positions
                show-movement-properties
                show-grid-lines]} @(current-data c)
        visible-tiles (camera/visible-tiles world-camera)
        [x y] (->tile (world-mouse-position c))]
    (draw-rectangle c x y 1 1 color/white)
    (when start-positions
      (doseq [[x y] visible-tiles
              :when (start-positions [x y])]
        (draw-filled-rectangle c x y 1 1 [1 1 1 0.1])))
    ; TODO move down to other doseq and make button
    (when show-movement-properties
      (doseq [[x y] visible-tiles
              :let [movement-property (movement-property tiled-map [x y])]]
        (draw-filled-circle c [(+ x 0.5) (+ y 0.5)]
                            0.08
                            color/black)
        (draw-filled-circle c [(+ x 0.5) (+ y 0.5)]
                            0.05
                            (case movement-property
                              "all"   color/green
                              "air"   color/orange
                              "none"  color/red))))
    (when show-grid-lines
      (draw-grid c 0 0 (tiled/width  tiled-map) (tiled/height tiled-map) 1 1 [1 1 1 0.5]))))

(defn- generate [{:keys [world-camera] :as context} properties]
  (let [{:keys [tiled-map
                area-level-grid
                start-positions]} (module-gen/generate context
                                                       (assoc properties
                                                              :creature-properties (all-properties context :creature)))
        atom-data (current-data context)]
    (dispose (:tiled-map @atom-data))
    (swap! atom-data assoc
           :tiled-map tiled-map
           :area-level-grid area-level-grid
           :start-positions (set start-positions))
    (show-whole-map! world-camera tiled-map)))

(defn edit-form [ctx [k v]]
  (doto (->text-field ctx (str v) {})
    (.setName (str k))))

(defn form-value [^com.badlogic.gdx.scenes.scene2d.ui.Table forms-table k]
  (.getText ^com.kotcrab.vis.ui.widget.VisTextField (.findActor forms-table (str k))))
; TODO reuse exactly property-editor window ( & generate separate window/button ?)

(defn- edn-edit-form [context edn-data-file]
  (let [properties (edn/read-string (slurp edn-data-file))
        table (->table context {})
        get-properties #(into {}
                              (for [k (keys properties)]
                                [k (edn/read-string (form-value table k))]))]
    (.colspan (.add table (->label context edn-data-file)) 2)
    (.row table)
    (doseq [[k v] properties]
      (.add table (->label context (name k)))
      (.add table ^com.badlogic.gdx.scenes.scene2d.Actor (edit-form context [k v]))
      (.row table))
    #_(.colspan (.add table (->text-button context
                                         (str "Save to file")
                                         (fn [_context]
                                           (spit edn-data-file
                                                 (with-out-str
                                                  (clojure.pprint/pprint
                                                   (get-properties)))))))
              2)
    [table get-properties]))

(defrecord SubScreen [current-data]
  gdl.disposable/Disposable
  (dispose [_]
    (dispose (:tiled-map @current-data)))

  gdl.screen/Screen
  (show [_ {:keys [world-camera]}]
    (show-whole-map! world-camera (:tiled-map @current-data)))

  (hide [_ {:keys [world-camera]}]
    (camera/reset-zoom! world-camera))

  (render [_ {:keys [world-camera] :as context}]
    (render-tiled-map context
                      (:tiled-map @current-data)
                      (constantly color/white))
    (render-world-view context render-on-map)
    (if (key-just-pressed? context input.keys/l)
      (swap! current-data update :show-grid-lines not))
    (if (key-just-pressed? context input.keys/m)
      (swap! current-data update :show-movement-properties not))
    (camera-controls context world-camera)
    (when (key-just-pressed? context input.keys/escape)
      (change-screen! :screens/main-menu))))

; TODO breaks when max-area-lvl > map-size
(defn screen [context]
  (let [window (->window context {:title "Properties"})
        [form get-properties] (edn-edit-form context "resources/maps/map.edn")] ; TODO move to properties
    (.add window ^com.badlogic.gdx.scenes.scene2d.Actor form)
    (.row window)
    (.add window (->text-button context "Generate" #(generate % (get-properties))))
    (.pack window)
    {:actors [window
              (->info-window context)]
     ; TODO checkboxes
     :sub-screen (->SubScreen (atom {:tiled-map (->tiled-map context module-gen/modules-file)
                                     :show-movement-properties false
                                     :show-grid-lines false}))}))

; TODO map-coords are clamped ? thats why showing 0 under and left of the map?
; make more explicit clamped-map-coords ?

; TODO
; leftest two tiles are 0 coordinate x
; and rightest is 16, not possible -> check clamping
; depends on screen resize or something, changes,
; maybe update viewport not called on resize sometimes
