(ns cdq.context.mouseover-entity
  (:require [gdl.context :refer [world-mouse-position mouse-on-stage-actor?]]
            [utils.core :refer [sort-by-order]]
            [cdq.context :refer [world-grid line-of-sight?]]
            [cdq.world.grid :refer [point->entities]]))

(defn- calculate-mouseover-entity [{:keys [context/player-entity
                                           context.entity/render-on-map-order]
                                    :as context}]
  (when-let [hits (point->entities (world-grid context)
                                   (world-mouse-position context))]
    ; TODO needs z-order ? what if 'shout' element or FX ?
    (->> render-on-map-order
         (sort-by-order hits #(:entity/z-order @%))
         reverse
         (filter #(line-of-sight? context @player-entity @%)) ; TODO here disable LoS for debug
         first)))

(extend-type gdl.context.Context
  cdq.context/MouseOverEntity
  (update-mouseover-entity [{:keys [context/mouseover-entity]
                             :as context}]
    (when-let [entity @mouseover-entity]
      (swap! entity dissoc :entity/mouseover?))
    (let [entity (if (mouse-on-stage-actor? context)
                   nil
                   (calculate-mouseover-entity context))]
      (reset! mouseover-entity entity)
      (when entity
        (swap! entity assoc :entity/mouseover? true)))))

(defn ->context []
  {:context/mouseover-entity (atom nil)})
