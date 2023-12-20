(ns game.world.grid)

(defprotocol Grid
  (cached-adjacent-cells [_ cell])
  (rectangle->cells [_ rectangle])
  (circle->cells    [_ circle])
  (circle->entities [_ circle])
  (add-entity!    [_ entity])
  (remove-entity! [_ entity]))


(defn- remove-from-occupied-cells! [entity]
  (doseq [cell (:occupied-cells @entity)]
    (swap! cell cell/remove-occupying-entity entity)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> width 1) (> height 1))
    (rectangle->cells grid rectangle)
    [(get grid
          [(int (+ (left-bottom 0) (/ width 2)))
           (int (+ (left-bottom 1) (/ height 2)))])]))

(defn- set-occupied-cells! [grid entity]
  (let [cells (rectangle->occupied-cells grid (:body @entity))]
    (doseq [cell cells]
      (swap! cell cell/add-occupying-entity entity))
    (swap! entity assoc :occupied-cells cells)))

(defn- set-cells! [entity new-cells]
  {:pre [(not-any? nil? new-cells)]}
  (swap! entity assoc :cells new-cells)
  (doseq [cell new-cells]
    (swap! cell cell/add-entity entity)))

; add
(add-entity!  [grid entity]
  (set-cells! entity (rectangle->cells grid (:body @entity)))
  (when (:is-solid @entity)
    (set-occupied-cells! grid entity)))

(defn- remove-from-cells! [entity]
  (doseq [cell (:cells @entity)]
    (swap! cell cell/remove-entity entity)))

; remove
(remove-entity! [_ entity]
                (remove-from-cells! entity)
                (when (:is-solid @entity)
                  (remove-from-occupied-cells! entity)))

(defn- update-cells! [e cells]
  (when-not (= cells (:cells @e))
    (remove-from-cells! e)
    (set-cells! e cells)))

; update entity = entity-position-changed! / make similar one for contentfields
; contentfields/cells/occupied-cells == world-connection component ???
; & id do @ ecs inside ?
(let [grid (world-grid context)]
      (update-cells! e (rectangle->cells grid (:body @e)))
      (when is-solid
        ; update-occupied-cells!
        (remove-from-occupied-cells! e)
        (set-occupied-cells! grid e)))

(defn valid-position? [grid entity*]
  (let [cells (rectangle->cells grid (:body entity*))]
    (and
     (not-any? #(cell/blocked? @% entity*) cells)
     (or (not (:is-solid (:body entity*)))
         (->> cells
              (map deref)
              cells->entities
              (not-any? #(and (not= (:id @%) (:id entity*))
                              (:is-solid (:body @%))
                              (geom/collides? (:body @%) (:body entity*)))))))))
