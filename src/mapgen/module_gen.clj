(ns mapgen.module-gen
  (:require [data.grid2d :as grid]
            [gdl.graphics :as g]
            [gdl.tiled :as tiled]
            [game.properties :as properties]
            [mapgen.utils :as utils]
            [mapgen.transitions :as transitions]
            [mapgen.movement-property :refer (movement-property)]
            [mapgen.cave-gen :as cave-gen]
            [mapgen.nad :as nad])
  (:import java.util.Random
           com.badlogic.gdx.graphics.g2d.TextureRegion
           [com.badlogic.gdx.maps.tiled TiledMap TiledMapTileLayer TiledMapTileLayer$Cell]
           [com.badlogic.gdx.maps.tiled.tiles StaticTiledMapTile]))

; "Tiles are usually shared by multiple cells."
; https://libgdx.com/wiki/graphics/2d/tile-maps#cells
; No copied-tile for AnimatedTiledMapTile yet (there was no copy constructor/method)
(def ^:private copy-tile
  (memoize
   (fn [^StaticTiledMapTile tile]
     (StaticTiledMapTile. tile))))

(defn- set-tile! [^TiledMapTileLayer layer [x y] tile]
  (let [cell (TiledMapTileLayer$Cell.)]
    (.setTile cell tile)
    (.setCell layer x y cell)))

(defn- add-layer! [tiled-map & {:keys [name visible properties]}]
  (let [layer (TiledMapTileLayer. (tiled/width  tiled-map)
                                  (tiled/height tiled-map)
                                  (tiled/get-property tiled-map :tilewidth)
                                  (tiled/get-property tiled-map :tileheight))]
    (.setName layer name)
    (when properties
      (.putAll (.getProperties layer) properties))
    (.setVisible layer visible)
    (.add (tiled/layers tiled-map) layer)
    layer))

(defn- make-tiled-map [grid ^TiledMap modules-tiled-map]
  (let [tiled-map (TiledMap.)
        properties (.getProperties tiled-map)]
    (.putAll properties (.getProperties modules-tiled-map)) ; tilewidth/tileheight
    (.put properties "width"  (grid/width  grid))
    (.put properties "height" (grid/height grid))
    (doseq [^TiledMapTileLayer layer (tiled/layers modules-tiled-map)
            :let [new-layer (add-layer! tiled-map
                                        :name (tiled/layer-name layer)
                                        :visible (.isVisible layer)
                                        :properties (.getProperties layer))]]
      (doseq [position (grid/posis grid)
              :let [{:keys [local-position tiled-map]} (get grid position)]
              :when local-position]
        (when-let [cell (tiled/cell-at local-position tiled-map layer)]
          (set-tile! new-layer
                     position
                     (copy-tile (.getTile cell))))))
    tiled-map))

(def modules-file "maps/modules.tmx")
(def ^:private module-width  32)
(def ^:private module-height 20)
(def ^:private module-offset 1)
(def ^:private transition-modules-offset-x 4)

(defn- module-index->local-positions [[module-x module-y]]
  (let [start-x (* module-x (+ module-width  module-offset))
        start-y (* module-y (+ module-height module-offset))]
    (for [x (range start-x (+ start-x module-width))
          y (range start-y (+ start-y module-height))]
      [x y])))

(defn- floor->module-index []
  ; TODO map consists of ~20 floor modules => shuffle (range 4) (range 4)
  ; -> no duplicates
  [(rand-int 4) ; !
   (rand-int 4)])

(defn- transition-idxvalue->module-index [idxvalue]
  [(+ (rem idxvalue 4) ; !
      transition-modules-offset-x)
   (int (/ idxvalue 4))])

(defn- place-module [modules unscaled-grid grid position {:keys [is-floor]}]
  (let [unscaled-position (mapv (comp int /) position [module-width module-height])
        idxvalue (if is-floor
                   0 ; !
                   (transitions/index-value unscaled-position
                                            #(= :wall (get unscaled-grid %))))
        local-positions (module-index->local-positions
                         (if is-floor
                           (floor->module-index)
                           (transition-idxvalue->module-index idxvalue)))
        offsets (for [x (range module-width)
                      y (range module-height)]
                  [x y])
        offset->local-position (zipmap offsets
                                       local-positions)]
    (reduce (fn [grid offset]
              (assoc grid
                     (mapv + position offset)
                     {:local-position (offset->local-position offset)
                      :tiled-map modules}))
            grid
            offsets)))

(defn- adjacent-wall-positions [grid]
  (filter (fn [p] (and (= :wall (get grid p))
                       (some #(not= :wall (get grid %))
                             (grid/get-8-neighbour-positions p))))
          (grid/posis grid)))

; TODO move this into gdl.module-tiledmap
(defn- place-modules [grid unscaled-grid positions]
  (let [modules (tiled/load-map modules-file) ; TODO not disposed
        _ (assert (and (= (tiled/width  modules) (* 8 (+ module-width  module-offset))) ; TODO hardcoded 8/4
                       (= (tiled/height modules) (* 4 (+ module-height module-offset)))))
        grid (reduce (fn [grid position] (place-module modules unscaled-grid grid position {:is-floor true}))
                     grid
                     positions)
        ;_ (println "adjacent walls: " (adjacent-wall-positions grid))
        grid (reduce (fn [grid position] (place-module modules unscaled-grid grid position {:is-floor false}))
                     grid
                     (map #(mapv * % [module-width module-height])
                           (adjacent-wall-positions unscaled-grid)))
        tiled-map (make-tiled-map grid modules)]
    ;(.dispose modules) ; TODO disposes of textures of tiles ...
    tiled-map))

(defn- make-grid [& {:keys [size]}]
  ; TODO generates 51,52. not max
  ; TODO can use different turn-ratio/depth/etc. params
  (let [{:keys [start grid]} (cave-gen/cave-gridgen (Random.) size size :wide)
        grid (nad/fix-not-allowed-diagonals grid)]
    {:start start
     :grid grid}))

; TODO can adjust:
; * split percentage , for higher level areas may scale faster (need to be more careful)
; * not 4 neighbors but just 1 tile randomwalk -> possible to have lvl 9 area next to lvl 1 ?
; * adds metagame to the game , avoid/or fight higher level areas, which areas to go next , etc...
; -> up to the player not step by step level increase like D2
; TODO can not only take first of added-p but multiples also
; can make parameter how fast it scales
(defn- area-level-grid
  "Expands from start position by adding one random adjacent neighbor.
  Each random walk is a step and is assigned a level as of max-level.
  (Levels are scaled, for example grid has 100 ground cells, so steps would be 0 to 100(99?)
  and max-level will smooth it out over 0 to max-level.
  The point of this is to randomize the levels so player does not have a smooth progression
  but can encounter higher level areas randomly around but there is always a path which goes from
  level 0 to max-level, so the player has to decide which areas to do in which order."
  [& {:keys [grid start max-level]}]
  (let [maxcount (->> grid
                      grid/cells
                      (filter #(= :ground %))
                      count)
        ; -> assume all :ground cells can be reached from start
        ; later check steps count == maxcount assert
        level-step (/ maxcount max-level)
        step->level #(int (Math/ceil (/ % level-step)))
        walkable-neighbours (fn [grid position]
                              (filter #(= (get grid %) :ground)
                                      (grid/get-4-neighbour-positions position)))]
    (loop [next-positions #{start}
           steps          [[0 start]]
           grid           (assoc grid start 0)]
      (let [next-positions (set
                            (filter #(seq (walkable-neighbours grid %))
                                    next-positions))]
        (if (seq next-positions)
          (let [p (rand-nth (seq next-positions))
                added-p (rand-nth (walkable-neighbours grid p))]
            (if added-p
              (let [area-level (step->level (count steps))]
                (recur (conj next-positions added-p)
                       (conj steps [area-level added-p])
                       (assoc grid added-p area-level)))
              (recur next-positions
                     steps
                     grid)))
          {:steps steps
           :grid  grid})))))

(comment

 ; TODO keep this working always for test !!
 ; pass max-size for printable here also
 ; TODO area-level-grid works better with more wide grids
 ; if the cave is very straight then it is just a continous progression and area-level-grid is useless

 (let [{:keys [start grid]} (make-grid :size 17)
       _ (utils/printgrid grid)
       _ (println)
       _ (utils/printgrid (assoc grid start 0))
       _ (println "width:  " (grid/width  grid)
                  "height: " (grid/height grid)
                  "start " start)
       _ (println (grid/posis grid)) ; TODO keys grid ?
       _ (utils/printgrid (reduce #(assoc %1 %2 nil)
                           grid
                           (adjacent-wall-positions grid)))
       {:keys [steps grid]} (area-level-grid :grid grid
                                             :start start
                                             :max-level 9)
       _ (utils/printgrid grid)])
)

(defn- creatures-with-level [level]
  (filter #(= level (:level %))
          (properties/get-all :creature)))

(def ^:private creature->tile
  (memoize
   (fn [{:keys [id image]}]
     (let [tile (StaticTiledMapTile. ^TextureRegion (:texture image))]
       (.put (.getProperties tile) "id" id)
       tile))))

(defn- creature-spawn-positions [spawn-rate tiled-map area-level-grid]
  (keep (fn [[position area-level]]
          (if (and (number? area-level)
                   (= "all" (movement-property tiled-map position)))
            ; module size 14x14 = 196 tiles, ca 5 monsters = 5/200 = 1/40
            (if (<= (rand) spawn-rate)
              (let [creatures (creatures-with-level area-level)]
                #_(println "Spawn creature with level " area-level)
                (when (seq creatures)
                  (let [creature (rand-nth creatures)
                        tile (creature->tile creature)]
                    [position
                     tile
                     ; get random monster with level
                     ; get tile of the monster
                     ]))))))
        area-level-grid))

; TODO use 'steps' ?
(defn- place-creatures! [spawn-rate tiled-map area-level-grid]
  (let [layer (add-layer! tiled-map
                          :name "creatures"
                          :visible true)]
    (doseq [[position tile] (creature-spawn-positions spawn-rate tiled-map area-level-grid)]
      (set-tile! layer position tile))))

(defn generate [{:keys [map-size
                        max-area-level
                        spawn-rate]}]
  (let [{:keys [start grid]} (make-grid :size map-size) ; TODO pass as arg
        ;_ (utils/printgrid grid) ; TODO logging where it is passed as arg
        ;_ (println)

        ; TODO even area-level-grid make into separate ns
        ; -> call @ world-gen-editor -> first make-grid (can render it too !)
        ; then area-level-grid -> render / show it
        ; then tiledmap (can randomize it also for same grid/area-level-grid
        ; then place creatures (also can keep randomizing)

        {:keys [steps grid]} (area-level-grid :grid grid
                                              :start start
                                              :max-level max-area-level)
        area-level-grid grid
        ;_ (utils/printgrid area-level-grid)
        scale [module-width module-height]

        scale-position #(mapv * (% 1) scale) ; step: [area-level position]
        module-placement-posis (map scale-position steps)
        unscaled-area-level-grid area-level-grid
        area-level-grid (utils/scale-grid area-level-grid
                                          scale)
        tiled-map (place-modules area-level-grid
                                 unscaled-area-level-grid ; => scaling happends inside place-modules !
                                 module-placement-posis)
        ; start-positions = positions in area level 0 (the starting module all positions)
        start-positions (map first
                             (filter (fn [[position area-level]]
                                       (and (number? area-level)
                                            (zero? area-level)))
                                     area-level-grid))]
    (place-creatures! spawn-rate
                      tiled-map ; TODO move out of this ns, use area-level-grid still
                      area-level-grid)
    {:tiled-map tiled-map
     :start-positions start-positions
     :area-level-grid area-level-grid}))
