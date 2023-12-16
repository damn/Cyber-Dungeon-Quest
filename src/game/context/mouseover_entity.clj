(ns game.context.mouseover-entity
  (:require [utils.core :refer [sort-by-order]]
            [game.render :refer [render-on-map-order]]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.cell-grid :refer (get-bodies-at-position)]))

(defn- calculate-mouseover-entity
  [{:keys [world-mouse-position
           context/player-entity
           context/world-map]
    :as context}]
  (let [cell-grid (:cell-grid world-map)
        hits (get-bodies-at-position cell-grid world-mouse-position)]
    ; TODO needs z-order ? what if 'shout' element or FX ?
    (when hits
      (->> render-on-map-order
           ; TODO re-use render-ingame code to-be-rendered-entities-on-map
           (sort-by-order hits #(:z-order @%))
           ; topmost body selected first, reverse of render-order
           reverse
           ; = same code @ which entities should get rendered...
           (filter #(in-line-of-sight? @player-entity @% context))
           first))))

(defn update-mouseover-entity [{:keys [context/mouseover-entity]
                                :as context}
                               mouse-over-ui-element?]
  (when-let [entity @mouseover-entity]
    (swap! entity dissoc :mouseover?))
  (let [entity (if mouse-over-ui-element?
                 nil
                 (calculate-mouseover-entity context))]
    (reset! mouseover-entity entity)
    (when entity
      (swap! entity assoc :mouseover? true))))
