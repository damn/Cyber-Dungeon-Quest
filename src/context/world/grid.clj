(ns context.world.grid
  (:require [data.grid2d :as grid2d]
            [gdl.math.geom :as geom]
            [utils.core :refer [->tile tile->middle]]
            [cdq.world.grid :refer [rectangle->cells circle->cells valid-position?]]
            [cdq.world.cell :as cell :refer [cells->entities]]))

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

(defn- set-cells! [grid entity]
  (let [cells (rectangle->cells grid (:entity/body @entity))]
    (assert (not-any? nil? cells))
    (swap! entity assoc :cells cells)
    (doseq [cell cells]
      (swap! cell cell/add-entity entity))))

(defn- remove-from-cells! [entity]
  (doseq [cell (:cells @entity)]
    (swap! cell cell/remove-entity entity)))

; old version, only calculating cells once, faster
; but anyway movement calculates it again -> refactor there first
#_(defn- update-cells! [grid entity]
  (let [cells (rectangle->cells grid (:entity/body @entity))]
    (when-not (= cells (:cells @entity))
      (remove-from-cells! entity)
      (set-cells! e cells))))

(defn- update-cells! [grid entity]
  (remove-from-cells! entity)
  (set-cells! grid entity))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> width 1) (> height 1))
    (rectangle->cells grid rectangle)
    [(get grid
          [(int (+ (left-bottom 0) (/ width 2)))
           (int (+ (left-bottom 1) (/ height 2)))])]))

(defn- set-occupied-cells! [grid entity]
  (let [cells (rectangle->occupied-cells grid (:entity/body @entity))]
    (doseq [cell cells]
      (swap! cell cell/add-occupying-entity entity))
    (swap! entity assoc :occupied-cells cells)))

(defn- remove-from-occupied-cells! [entity]
  (doseq [cell (:occupied-cells @entity)]
    (swap! cell cell/remove-occupying-entity entity)))

(defn- update-occupied-cells! [grid entity]
  (remove-from-occupied-cells! entity)
  (set-occupied-cells! grid entity))

(extend-type data.grid2d.Grid2D
  cdq.world.grid/Grid
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
         (rectangle->cells grid)))

  (circle->entities [grid circle]
    (->> (circle->cells grid circle)
         (map deref)
         cells->entities
         (filter #(geom/collides? circle (:entity/body @%)))))

  (point->entities [grid position]
    (when-let [cell (get grid (->tile position))]
      (filter #(geom/point-in-rect? position (:entity/body @%))
              (:entities @cell))))

  (valid-position? [grid entity*]
    (let [cells* (map deref (rectangle->cells grid (:entity/body entity*)))]
      (and (not-any? #(cell/blocked? % entity*) cells*)
           (or (not (:is-solid (:entity/body entity*)))
               (->> cells*
                    cells->entities
                    (map deref)
                    (not-any? #(and (not= (:id %) (:id entity*))
                                    (:is-solid (:entity/body %))
                                    (geom/collides? (:entity/body %)
                                                    (:entity/body entity*)))))))))

  (add-entity! [grid entity]
    ;(assert (valid-position? grid @entity)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (set-cells! grid entity)
    (when (:is-solid (:entity/body @entity))
      (set-occupied-cells! grid entity)))

  (remove-entity! [_ entity]
    (remove-from-cells! entity)
    (when (:is-solid (:entity/body @entity))
      (remove-from-occupied-cells! entity)))

  (entity-position-changed! [grid entity]
    ;(assert (valid-position? grid @entity)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (update-cells! grid entity)
    (when (:is-solid (:entity/body @entity))
      (update-occupied-cells! grid entity))))

; TODO separate ns?

(defrecord Cell [position
                 middle ; TODO needed ?
                 adjacent-cells
                 movement
                 entities
                 occupied
                 ; TODO potential-field ? PotentialFieldCell ?
                 good
                 evil]
  cdq.world.cell/Cell
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

  (blocked? [_ {:keys [entity/is-flying]}]
    (case movement
      :none true
      :air (not is-flying)
      :all false))

  (occupied-by-other? [_ entity]
    (seq (disj occupied entity)))

  (nearest-entity [this faction]
    (-> this faction :entity))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

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
