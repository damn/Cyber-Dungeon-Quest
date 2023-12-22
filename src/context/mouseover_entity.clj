(ns context.mouseover-entity
  (:require [gdl.context :refer [world-mouse-position mouse-on-stage-actor?]]
            [utils.core :refer [sort-by-order]]
            [game.context :refer [world-grid line-of-sight?]]
            [game.world.grid :refer [point->entities]]))

(defn- calculate-mouseover-entity [{:keys [context/player-entity
                                           context.ecs/render-on-map-order]
                                    :as context}]
  (when-let [hits (point->entities (world-grid context)
                                   (world-mouse-position context))]
    ; TODO needs z-order ? what if 'shout' element or FX ?
    (->> render-on-map-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? context @player-entity @%))
         first)))

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

(defn ->context []
  {:context/mouseover-entity (atom nil)})
