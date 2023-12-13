(ns game.maps.cell-grid
  (:require [gdl.maps.tiled :as tiled]
            [gdl.math.geom :as geom]
            [gdl.math.vector :as v]
            [gdl.math.raycaster :as raycaster]
            [data.grid2d :as grid]
            [utils.core :refer [translate-to-tile-middle int-posi diagonal-direction?]]
            [game.session :as session]
            [mapgen.movement-property :refer (movement-property)]))

(defrecord Cell [position
                 middle
                 adjacent-cells
                 movement
                 entities
                 occupied
                 good
                 evil])

(defn- create-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (atom
   (map->Cell
    {:position position
     :middle (translate-to-tile-middle position)
     :movement movement
     :entities #{}
     :occupied #{}})))

(defn occupied-by-other?
  "returns true if there is some solid body with center-tile = this cell
   or a multiple-cell-size body which touches this cell."
  [cell entity]
  (seq (disj (:occupied @cell) entity)))

(defn cell-blocked?
  ([cell]
   (cell-blocked? cell {}))
  ([cell {:keys [is-flying]}]
   (or (nil? cell)
       (case (:movement @cell)
         :none true
         :air (not is-flying)
         :all false))))

(defn fast-cell-blocked? [cell*]
  (or (nil? cell*)
      (case (:movement cell*)
        :none true
        :air true
        :all false)))

(defn cached-get-adjacent-cells [cell-grid cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (map cell-grid (-> @cell
                                    :position
                                    grid/get-8-neighbour-positions))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn- set-cell-blocked-boolean-array [arr cell]
  (let [[x y] (:position @cell)]
    (aset arr
          x
          y
          (boolean (cell-blocked? cell {:is-flying true})))))

(defn create-cell-blocked-boolean-array [grid]
  (let [arr (make-array Boolean/TYPE
                        (grid/width grid)
                        (grid/height grid))]
    (doseq [cell (grid/cells grid)]
      (set-cell-blocked-boolean-array arr cell))
    arr))

(defn get-entities [cell]
  (if cell
    (:entities @cell)
    #{}))

(defn get-entities-from-cells [cells]
  (distinct (mapcat get-entities cells)))

(defn- in-cell? [cell entity]
  (get (get-entities cell) entity))

(defn add-entity! [cell entity]
  {:pre [(not (in-cell? cell entity))]}
  (swap! cell update :entities conj entity))

(defn remove-entity! [cell entity]
  {:pre [(in-cell? cell entity)]}
  (swap! cell update :entities disj entity))

(defn is-diagonal? [from to]
  (let [[fx fy] (:position @from)
        [tx ty] (:position @to)
        xdir (- tx fx)
        ydir (- ty fy)]
    (diagonal-direction? [xdir ydir])))

(defn create-grid-from-tiledmap [tiled-map]
  (grid/create-grid (tiled/width  tiled-map)
                    (tiled/height tiled-map)
                    (fn [position]
                      (create-cell position
                                   (case (movement-property tiled-map position)
                                     "none" :none
                                     "air"  :air
                                     "all"  :all)))))

(defn- rectangle->touched-tiles
  [{[x y] :left-bottom :keys [left-bottom width height]}]
  {:pre [left-bottom width height]}
  (let [x       (float x)
        y       (float y)
        width   (float width)
        height  (float height)
        l (int x)
        b (int y)
        r (int (+ x width))
        t (int (+ y height))]
    (distinct
     (if (or (> width 1) (> height 1))
       (for [x (range l (inc r))
             y (range b (inc t))]
         [x y])
       [[l b] [l t] [r b] [r t]]))))

(defn rectangle->touched-cells [cell-grid rectangle]
  (->> rectangle
       rectangle->touched-tiles
       (map cell-grid)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn rectangle->occupied-cells [cell-grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> width 1) (> height 1))
    (rectangle->touched-cells cell-grid rectangle)
    [(get cell-grid
          [(int (+ (left-bottom 0) (/ width 2)))
           (int (+ (left-bottom 1) (/ height 2)))])]))

(defn inside-cell? [cell-grid entity* cell]
  (let [touched-cells (rectangle->touched-cells cell-grid (:body entity*))]
    (and (= 1 (count touched-cells))
         (= cell (first touched-cells)))))

(defn circle->touched-cells [cell-grid circle]
  (->> circle
       geom/circle->outer-rectangle
       (rectangle->touched-cells cell-grid)))

(defn circle->touched-entities [cell-grid circle]
  (->> (circle->touched-cells cell-grid circle)
       get-entities-from-cells
       (filter #(geom/collides? circle (:body @%)))))

(defn get-bodies-at-position [cell-grid position]
  (when-let [cell (get cell-grid (mapv int position))]
    (filter #(geom/point-in-rect? position (:body @%))
            (get-entities cell))))

(defn ray-blocked? [{:keys [cell-blocked-boolean-array width height]} start target]
  (raycaster/ray-blocked? cell-blocked-boolean-array width height start target))

(defn- create-double-ray-endpositions
  "path-w in tiles."
  [[start-x start-y] [target-x target-y] path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/get-normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(defn is-path-blocked?
  "path-w in tiles. casts two rays."
  [world-map start target path-w]
  (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
    (or
     (ray-blocked? world-map start1 target1)
     (ray-blocked? world-map start2 target2))))
