(ns mapgen.module-gen
  (:require [data.grid2d :as grid]
            [gdl.maps.tiled :as tiled]
            [gdl.context :refer [->tiled-map]]
            [cdq.context :refer [all-properties]]
            [mapgen.utils :as utils]
            [mapgen.tiled-utils :refer [->static-tiled-map-tile set-tile! put! add-layer! grid->tiled-map]]
            [mapgen.transitions :as transitions]
            [mapgen.movement-property :refer (movement-property)]
            [mapgen.cave-gen :as cave-gen]
            [mapgen.nad :as nad])
  (:import java.util.Random))

; TODO HERE
; * only spawn in reachable tiles (flood fill)
; * spawn in transition tiles / set area levels too
; * unique max 16 modules, not random take @ #'floor->module-index
; * check not using start module with hill and spawning on hill ?

(comment
 ; TODO use same codepath here as in 'generate'
 ; just with logging enabled
 ; => can make sure to debug it
 (let [{:keys [start grid]} (make-grid :size 150)
       _ (println "BASE GRID:\n")
       _ (utils/printgrid grid)
       _ (println)
       _ (println "WITH START POSITION (0) :\n")
       _ (utils/printgrid (assoc grid start 0))
       _ (println "\nwidth:  " (grid/width  grid)
                  "height: " (grid/height grid)
                  "start " start "\n")
       ;_ (println (grid/posis grid))
       _ (println "With adjacent-wall-positions marked:\n")
       _ (utils/printgrid (reduce #(assoc %1 %2 nil)
                                  grid
                                  (adjacent-wall-positions grid)))
       {:keys [steps grid]} (area-level-grid :grid grid
                                             :start start
                                             :max-level 9)
       _ (println "\n With area levels: \n")
       _ (utils/printgrid grid)])
 )

(def modules-file "maps/modules.tmx")
(def module-width  32)
(def module-height 20)
(def ^:private number-modules-x 8)
(def ^:private number-modules-y 4)
(def ^:private module-offset-tiles 1)
(def ^:private transition-modules-row-width 4) ; in one row in the modules-tiled-map
(def ^:private transition-modules-row-height 4) ; in one row in the modules-tiled-map
(def ^:private transition-modules-offset-x 4)
(def ^:private floor-modules-row-width 4)
(def ^:private floor-modules-row-height 4)

(defn- module-index->local-positions [[module-x module-y]]
  (let [start-x (* module-x (+ module-width  module-offset-tiles))
        start-y (* module-y (+ module-height module-offset-tiles))]
    (for [x (range start-x (+ start-x module-width))
          y (range start-y (+ start-y module-height))]
      [x y])))

(defn- floor->module-index []
  [(rand-int floor-modules-row-width)
   (rand-int floor-modules-row-height)])

(defn- transition-idxvalue->module-index [idxvalue]
  [(+ (rem idxvalue transition-modules-row-width)
      transition-modules-offset-x)
   (int (/ idxvalue transition-modules-row-height))])

(def ^:private floor-idxvalue 0)

(defn- place-module [unscaled-grid grid position & {:keys [is-floor]}]
  (let [unscaled-position (mapv (comp int /) position [module-width module-height])
        ; wrong name, its 'wall?/other-tileset-thigny'
        position->transition? #(= :wall (get unscaled-grid %))
        idxvalue (if is-floor
                   floor-idxvalue
                   (transitions/index-value unscaled-position
                                            position->transition?))
        ; wrong name not local-position but modules-tiled-map-position
        local-positions (module-index->local-positions
                         (if is-floor
                           (floor->module-index)
                           (transition-idxvalue->module-index idxvalue)))
        offsets (for [x (range module-width)
                      y (range module-height)]
                  [x y])
        offset->local-position (zipmap offsets local-positions)]
    (reduce (fn [grid offset]
              (assoc grid
                     (mapv + position offset)
                     (offset->local-position offset)))
            grid
            offsets)))

(defn- adjacent-wall-positions [grid]
  (filter (fn [p] (and (= :wall (get grid p))
                       (some #(not= :wall (get grid %))
                             (grid/get-8-neighbour-positions p))))
          (grid/posis grid)))

(defn- place-modules [modules-tiled-map grid unscaled-grid positions] ; <= scaled-area-level-grid unscaled-area-level-grid
  (let [_ (assert (and (= (tiled/width  modules-tiled-map)
                          (* number-modules-x (+ module-width  module-offset-tiles)))
                       (= (tiled/height modules-tiled-map)
                          (* number-modules-y (+ module-height module-offset-tiles)))))
        grid (reduce (fn [grid position]
                       (place-module unscaled-grid grid position :is-floor true))
                     grid
                     positions)
        ;_ (println "adjacent walls: " (adjacent-wall-positions grid))
        grid (reduce (fn [grid position]
                       (place-module unscaled-grid grid position :is-floor false))
                     grid
                     (map #(mapv * % [module-width module-height])
                          (adjacent-wall-positions unscaled-grid)))]
    (grid->tiled-map modules-tiled-map grid)))

(defn- make-grid [& {:keys [size]}]
  ; TODO generates 51,52. not max 50
  ; TODO can use different turn-ratio/depth/etc. params
  (let [{:keys [start grid]} (cave-gen/cave-gridgen (Random.) size size :wide)
        grid (nad/fix-not-allowed-diagonals grid)]
    {:start start
     :grid grid}))

; can adjust:
; * split percentage , for higher level areas may scale faster (need to be more careful)
; * not 4 neighbors but just 1 tile randomwalk -> possible to have lvl 9 area next to lvl 1 ?
; * adds metagame to the game , avoid/or fight higher level areas, which areas to go next , etc...
; -> up to the player not step by step level increase like D2
; can not only take first of added-p but multiples also
; can make parameter how fast it scales
; area-level-grid works better with more wide grids
; if the cave is very straight then it is just a continous progression and area-level-grid is useless
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

(defn- creatures-with-level [creature-properties level]
  (filter #(= level (:level %)) creature-properties))

(def ^:private creature->tile
  (memoize
   (fn [{:keys [id image]}]
     (assert (and id image))
     (let [tile (->static-tiled-map-tile (:texture image))]
       (put! (tiled/properties tile) "id" id)
       tile))))

(defn- creature-spawn-positions [creature-properties spawn-rate tiled-map area-level-grid]
  (keep (fn [[position area-level]]
          (if (and (number? area-level)
                   (= "all" (movement-property tiled-map position))
                   (<= (rand) spawn-rate))
            (let [creatures (creatures-with-level creature-properties area-level)]
              (when (seq creatures)
                [position (creature->tile (rand-nth creatures))]))))
        area-level-grid))

(defn- place-creatures! [context spawn-rate tiled-map area-level-grid]
  (let [layer (add-layer! tiled-map
                          :name "creatures"
                          :visible true)
        creature-properties (all-properties context :creature)]
    (doseq [[position tile] (creature-spawn-positions creature-properties spawn-rate tiled-map area-level-grid)]
      (set-tile! layer position tile))))

(defn generate
  "The generated tiled-map needs to be disposed."
  [context {:keys [map-size max-area-level spawn-rate]}]
  (assert (<= max-area-level map-size))
  (let [{:keys [start grid]} (make-grid :size map-size)
        ;_ (utils/printgrid grid)
        ;_ (println)
        {:keys [steps grid]} (area-level-grid :grid grid
                                              :start start
                                              :max-level max-area-level)
        area-level-grid grid
        ;_ (utils/printgrid area-level-grid)
        scale [module-width module-height]
        scale-position #(mapv * (% 1) scale) ; step: [area-level position]
        module-placement-posis (map scale-position steps)
        unscaled-area-level-grid area-level-grid
        area-level-grid (utils/scale-grid area-level-grid scale)
        tiled-map (place-modules (->tiled-map context modules-file)
                                 area-level-grid ; = scaled area level gri
                                 unscaled-area-level-grid ; => scaling happends inside place-modules !
                                 module-placement-posis)
        ; start-positions = positions in area level 0 (the starting module all positions)
        start-positions (map first
                             (filter (fn [[position area-level]]
                                       (and (number? area-level)
                                            (zero? area-level)))
                                     area-level-grid))
        princess-position (rand-nth
                           (map first
                                (filter (fn [[position area-level]]
                                          (and (number? area-level)
                                               (= max-area-level area-level)
                                               (#{:no-cell :undefined}
                                                (tiled/property-value tiled-map :creatures position :id))))
                                        area-level-grid)))]
    (place-creatures! context spawn-rate tiled-map area-level-grid)
    (println "princess " princess-position)
    (if princess-position
      (set-tile! (tiled/get-layer tiled-map "creatures")
                 princess-position
                 (creature->tile (cdq.context/get-property context :lady-a)))
      (println "NO PRINCESS POSITION FOUND"))
    {:tiled-map tiled-map
     :start-positions start-positions
     :area-level-grid area-level-grid}))
