(ns context.world.content-grid
  (:require [data.grid2d :as grid2d]
            gdl.context
            [game.context :refer [content-grid]]
            game.world.content-grid))

(defrecord ContentGrid [grid cell-w cell-h]
  game.world.content-grid/ContentGrid
  (update-entity! [_ entity]
    (let [{:keys [entity/content-cell position]} @entity
          [x y] position
          new-cell (get grid [(int (/ x cell-w))
                              (int (/ y cell-h))])]
      (when-not (= content-cell new-cell)
        (swap! new-cell update :entities conj entity)
        (swap! entity assoc :entity/content-cell new-cell)
        (when content-cell
          (swap! content-cell update :entities disj entity)))))

  (remove-entity! [_ entity]
    (-> @entity
        :entity/content-cell
        (swap! update :entities disj entity)))

  (get-active-entities [_ center-entity]
    (->> (let [idx (-> @center-entity
                       :entity/content-cell
                       deref
                       :idx)]
           (cons idx (grid2d/get-8-neighbour-positions idx)))
         (keep grid)
         (mapcat (comp :entities deref)))))

(defn ->content-grid [w h cell-w cell-h]
  (->ContentGrid (grid2d/create-grid (inc (int (/ w cell-w))) ; inc because corners
                                     (inc (int (/ h cell-h)))
                                     (fn [idx]
                                       (atom {:idx idx,
                                              :entities #{}})))
                 cell-w
                 cell-h))

(comment

 (defn get-all-entities-of-current-map [context]
   (mapcat #(deref (:entities %))
           (grid2d/cells (:content-grid (:context/world-map context)))))

 (count
  (get-all-entities-of-current-map @app.state/current-context))

 )
