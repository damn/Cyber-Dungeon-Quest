(ns mapgen.module-gen
  (:require [data.grid2d :as grid]
            [gdl.maps.tiled :as tiled]
            [gdl.context :refer [->tiled-map]]
            [mapgen.utils :as utils]
            [mapgen.tiled-utils :refer [copy-tile ->static-tiled-map-tile set-tile! cell->tile ->empty-tiled-map put! put-all! visible?  add-layer!]]
            [mapgen.transitions :as transitions]
            [mapgen.movement-property :refer (movement-property)]
            [mapgen.cave-gen :as cave-gen]
            [mapgen.nad :as nad])
  (:import java.util.Random))

; TODO HERE
; * spawn in transition tiles / set area levels too
; * only spawn in reachable tiles (flood fill)
; * unique max 16 modules, not random take

(defn- generate-tiled-map
  "Creates an empty new tiled-map with same layers and properties as modules-tiled-map.
The size of the map is as of the grid, which contains also the tile information from the modules-tiled-map."
  [modules-tiled-map grid]
  (let [tiled-map (->empty-tiled-map)
        properties (tiled/properties tiled-map)]
    (put-all! properties (tiled/properties modules-tiled-map))
    (put! properties "width"  (grid/width  grid))
    (put! properties "height" (grid/height grid))
    (doseq [layer (tiled/layers modules-tiled-map)
            :let [new-layer (add-layer! tiled-map
                                        :name (tiled/layer-name layer)
                                        :visible (visible? layer)
                                        :properties (tiled/properties layer))]]
      (doseq [position (grid/posis grid)
              :let [local-position (get grid position)]
              :when local-position]
        (when-let [cell (tiled/cell-at modules-tiled-map layer local-position)]
          (set-tile! new-layer
                     position
                     (copy-tile (cell->tile cell))))))
    tiled-map))

(def modules-file "maps/modules.tmx")
(def module-width  32)
(def module-height 20)
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

(defn- place-module [unscaled-grid grid position {:keys [is-floor]}]
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
                     (offset->local-position offset)))
            grid
            offsets)))

(defn- adjacent-wall-positions [grid]
  (filter (fn [p] (and (= :wall (get grid p))
                       (some #(not= :wall (get grid %))
                             (grid/get-8-neighbour-positions p))))
          (grid/posis grid)))

(defn- place-modules [ctx grid unscaled-grid positions]
  (let [modules (->tiled-map ctx modules-file)
        _ (assert (and (= (tiled/width  modules) (* 8 (+ module-width  module-offset))) ; TODO hardcoded 8/4
                       (= (tiled/height modules) (* 4 (+ module-height module-offset)))))
        grid (reduce (fn [grid position] (place-module unscaled-grid grid position {:is-floor true}))
                     grid
                     positions)
        ;_ (println "adjacent walls: " (adjacent-wall-positions grid))
        grid (reduce (fn [grid position] (place-module unscaled-grid grid position {:is-floor false}))
                     grid
                     (map #(mapv * % [module-width module-height])
                          (adjacent-wall-positions unscaled-grid)))]
    (generate-tiled-map modules grid)))

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
                   (= "all" (movement-property tiled-map position)))
            ; module size 14x14 = 196 tiles, ca 5 monsters = 5/200 = 1/40
            (if (<= (rand) spawn-rate)
              (let [creatures (creatures-with-level creature-properties area-level)]
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
(defn- place-creatures! [creature-properties spawn-rate tiled-map area-level-grid]
  (let [layer (add-layer! tiled-map
                          :name "creatures"
                          :visible true)]
    (doseq [[position tile] (creature-spawn-positions creature-properties spawn-rate tiled-map area-level-grid)]
      (set-tile! layer position tile))))

; TODO assert max-area-level <= map-size (check map size again if correct # of cells)
(defn generate
  "The generated tiled-map needs to be disposed."
  [ctx
   {:keys [creature-properties
           map-size
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
        tiled-map (place-modules ctx
                                 area-level-grid
                                 unscaled-area-level-grid ; => scaling happends inside place-modules !
                                 module-placement-posis)
        ; start-positions = positions in area level 0 (the starting module all positions)
        start-positions (map first
                             (filter (fn [[position area-level]]
                                       (and (number? area-level)
                                            (zero? area-level)))
                                     area-level-grid))

        ; ! TODO PRINCESS WAS ON THE HILL !
        princess-position (rand-nth
                           (map first
                                (filter (fn [[position area-level]]
                                          (and (number? area-level)
                                               (= max-area-level area-level)
                                               (#{:no-cell :undefined}
                                                (tiled/property-value tiled-map
                                                                      :creatures
                                                                      position
                                                                      :id))))
                                        area-level-grid)))]
    (place-creatures! creature-properties
                      spawn-rate
                      tiled-map ; TODO move out of this ns, use area-level-grid still
                      area-level-grid)
    (println "princess " princess-position)
    (if princess-position
      (set-tile! (tiled/get-layer tiled-map "creatures")
                 princess-position
                 (creature->tile (cdq.context/get-property ctx :lady-a)))
      (println "NO PRINCESS POSITION FOUND") ; TODO map too small for max area level ! assert !
      )
    {:tiled-map tiled-map
     :start-positions start-positions
     :area-level-grid area-level-grid}))
