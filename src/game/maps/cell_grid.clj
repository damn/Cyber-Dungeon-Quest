(ns game.maps.cell-grid
  (:require [clojure.set :as set]
            [gdl.geom :as geom]
            [gdl.vector :as v]
            [gdl.tiled :as tiled]
            [gdl.raycaster :as raycaster]
            [data.grid2d :as grid]
            [utils.core :refer [translate-to-tile-middle int-posi diagonal-direction?]]
            [game.session :as session]
            [game.maps.data :refer (get-current-map-data)]
            [mapgen.movement-property :refer (movement-property)]))

(defn get-cell-grid []
  (:cell-grid (get-current-map-data)))

(defn world-width  [] (grid/width  (get-cell-grid)))
(defn world-height [] (grid/height (get-cell-grid)))

(defn- get-cell-blocked-boolean-array []
  (:cell-blocked-boolean-array (get-current-map-data)))

(defrecord Cell [position
                 middle
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
     :occupied #{}}))) ; only :is-solid entities in occupied  (assert?)

(defn occupied-by-other?
  "returns true if there is some solid body with center-tile = this cell
   or a multiple-cell-size body which touches this cell."
  [cell entity]
  (seq (disj (:occupied @cell) entity)))

; use cell* ? but is more convenient like this

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

(defn get-cell
  "returns nil when not existing cell (outside map bounds or undefined area (behind walls))"
  [posi]
  (get (get-cell-grid) (int-posi posi))) ; TODO mapv int ...

(defn get-cells
  "see (doc get-cell). Use int-posis for this! Faster than (map get-cell posis)"
  [posis]
  (map (get-cell-grid) posis))

;TODO just use a delay/force?
(defn cached-get-adjacent-cells [cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (-> @cell
                     :position
                     grid/get-8-neighbour-positions
                     get-cells)]
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

(defn inside-map? [tile]
  (get-cell tile))

(def ^:private listeners (atom []))

(def state (reify session/State
             (load! [_ data]
               (reset! listeners []))
             (serialize [_])
             (initial-data [_])))

#_(defn add-cell-blocks-changed-listener [f]
  (swap! listeners conj f))

#_(defn cell-blocks-changed-update-listeners []
  (runmap #(%) @listeners)) ; TODO doseq

#_(defn change-cell-blocks
  "do not change to blocking while game running or bodies may be walled in. (( ; TODO why ? ))
  after changing update the listeners!!" ; vlt cell-seq �bergeben und danach wird hier update-listeners gecallt.
  [cell new-blocks]
  (swap! cell assoc :blocks new-blocks)
  (set-cell-blocked-boolean-array (get-cell-blocked-boolean-array)
                                  cell))


; many entities
; performance bottleneck #1 is dereffing cells !!
; => read more often than write?
; => all grid as 1 atom ??
; simple lookup
; but potential field
; => also 'maps.grid' / grid/get , etc.
; => grid is 1 atom !

(defn get-entities [cell]
  (if cell
    (:entities @cell)
    #{}))

(defn get-entities-from-cells [cells]
  (distinct (mapcat get-entities cells)))

(defn- in-cell? [cell entity]
  (get (get-entities cell) entity))

(defn add-entity [cell entity]
  {:pre [(not (in-cell? cell entity))]}
  (swap! cell update :entities conj entity))

(defn remove-entity [cell entity]
  {:pre [(in-cell? cell entity)]}
  (swap! cell update :entities disj entity))

;;

; TODO use mapgen.nad code?
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

; TODO nil cells behind walls is just a memory optimization (maybe not necessary ?)
; do not make code in mapgen more complicated because of that -> do it here only


; TODO all this pass body & not entity*

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
       (list [l b] [l t] [r b] [r t])))))

(def rectangle->touched-cells
  (comp get-cells
        rectangle->touched-tiles))

; TODO give :center / :left-bottom / :width / :height / :radius
; :half-width / :half-height ...
; can also be a defrecord ...
; funny so now it has a type ?
(defn rectangle->occupied-cells [{:keys [left-bottom width height] :as rectangle}]
  (if (or (> width 1) (> height 1))
    (rectangle->touched-cells rectangle) ; TODO use inside tiles (size 4.5 use 4x4 ? )
    [(get-cell [(+ (left-bottom 0) (/ width 2))
                (+ (left-bottom 1) (/ height 2))
                ])]))

(defn inside-cell? [entity* cell] ; TODO rectangle, not entity*
  (let [touched-cells (rectangle->touched-cells (:body entity*))]
    (and (= 1 (count touched-cells))
         (= cell (first touched-cells)))))

(def ^:private circle->touched-cells
  (comp rectangle->touched-cells
        geom/circle->outer-rectangle))

; unused
#_(defn rectangle->touched-entities [rectangle]
  (->> rectangle
       rectangle->touched-cells
       get-entities-from-cells
       (filter #(geom/collides? rectangle @%))))

(defn circle->touched-entities [circle]
  (->> circle
       circle->touched-cells
       get-entities-from-cells
       (filter #(geom/collides? circle (:body @%)))))

; get-entities-at-point
; entities-at-position or float-position->entities, entities-at-float-position
(defn get-bodies-at-position [position]
  (when-let [cell (get-cell position)]
    (filter #(geom/point-in-rect? position (:body @%))
            (get-entities cell))))

(defn ray-blocked? [start target]
  (raycaster/ray-blocked? (get-cell-blocked-boolean-array)
                          (world-width)
                          (world-height)
                          start
                          target))

(defn- create-double-ray-endpositions
  "path-w in tiles."
  [start-x,start-y,target-x,target-y,path-w]
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
  ([start-x,start-y,target-x,target-y,path-w]
    (let [[start1,target1,start2,target2] (create-double-ray-endpositions start-x start-y target-x target-y path-w)]
      (is-path-blocked? start1 target1 start2 target2)))
  ([start1,target1,start2,target2]
    (or
      (ray-blocked? start1 target1)
      (ray-blocked? start2 target2))))
