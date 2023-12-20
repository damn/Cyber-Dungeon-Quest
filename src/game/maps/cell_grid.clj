(ns game.maps.cell-grid
  (:require [gdl.math.geom :as geom]
            [data.grid2d :as grid]
            [utils.core :refer [diagonal-direction?]]))

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
