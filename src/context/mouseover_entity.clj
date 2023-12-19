(ns context.mouseover-entity
  (:require [gdl.context :refer [world-mouse-position mouse-on-stage-actor?]]
            game.context
            [utils.core :refer [sort-by-order]]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.cell-grid :refer (get-bodies-at-position)]))

(defn- calculate-mouseover-entity
  [{:keys [context/player-entity
           context/render-on-map-order
           context/world-map]
    :as context}]
  (let [cell-grid (:cell-grid world-map)
        hits (get-bodies-at-position cell-grid (world-mouse-position context))]
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

(extend-type gdl.context.Context
  game.context/MouseOverEntity
  (update-mouseover-entity [{:keys [context/mouseover-entity]
                             :as context}]
    (when-let [entity @mouseover-entity]
      (swap! entity dissoc :mouseover?))
    (let [entity (if (mouse-on-stage-actor? context)
                   nil
                   (calculate-mouseover-entity context))]
      (reset! mouseover-entity entity)
      (when entity
        (swap! entity assoc :mouseover? true)))))

(defn ->context-map []
  {:context/mouseover-entity (atom nil)})
