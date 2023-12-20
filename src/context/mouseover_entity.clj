(ns context.mouseover-entity
  (:require [gdl.context :refer [world-mouse-position mouse-on-stage-actor?]]
            [game.context :refer [entities-at-position line-of-sight?]]
            [utils.core :refer [sort-by-order]]))

(defn- calculate-mouseover-entity [{:keys [context/player-entity
                                           context/render-on-map-order]
                                    :as context}]
  (when-let [hits (entities-at-position context (world-mouse-position context))]
    ; TODO needs z-order ? what if 'shout' element or FX ?
    (->> render-on-map-order
         ; TODO re-use render-ingame code to-be-rendered-entities-on-map
         (sort-by-order hits #(:z-order @%))
         ; topmost body selected first, reverse of render-order
         reverse
         ; = same code @ which entities should get rendered...
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

(defn ->context-map []
  {:context/mouseover-entity (atom nil)})
