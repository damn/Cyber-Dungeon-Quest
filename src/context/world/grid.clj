(ns context.world.grid
  (:require [data.grid2d :as grid2d]
            [gdl.math.geom :as geom]
            [utils.core :refer [tile->middle]]
            [game.world.grid :refer [rectangle->cells]]
            game.world.cell))

(defn- rectangle->tiles
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

(extend-type data.grid2d.Grid2D
  game.world.grid/Grid
  (cached-adjacent-cells [grid cell]
    (if-let [result (:adjacent-cells @cell)]
      result
      (let [result (keep grid (-> @cell
                                       :position
                                       grid2d/get-8-neighbour-positions))]
        (swap! cell assoc :adjacent-cells result)
        result)))

  (rectangle->cells [grid rectangle]
    (->> rectangle
         rectangle->tiles
         (keep grid)))

  (circle->cells [grid circle]
    (->> circle
         geom/circle->outer-rectangle
         (rectangle->cells grid))))

(defrecord Cell [position
                 middle ; TODO needed ?
                 adjacent-cells
                 movement
                 entities
                 occupied

                 ; TODO potential-field ? PotentialFieldCell ?
                 good
                 evil]
  game.world.cell/Cell
  (add-entity [this entity]
    (assert (not (get entities entity)))
    (update this :entities conj entity))

  (remove-entity [this entity]
    (assert (get entities entity))
    (update this :entities disj entity))

  (add-occupying-entity [this entity]
    (assert (not (get occupied entity)))
    (update this :occupied conj entity))

  (remove-occupying-entity [this entity]
    (assert (get occupied entity))
    (update this :occupied disj entity))

  (blocked? [_]
    (= :none movement))

  (blocked? [_ {:keys [is-flying]}]
    (case movement
      :none true
      :air (not is-flying)
      :all false))

  (occupied-by-other? [_ entity]
    (seq (disj occupied entity))))

(defn- create-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->Cell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn create-grid [width height position->value]
  (grid2d/create-grid width height
                      #(->> %
                            position->value
                            (create-cell %)
                            atom)))
