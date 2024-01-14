(ns cdq.context.game
  (:require [cdq.context.counter :as counter]
            [cdq.context.ecs :as ecs]
            [cdq.context.mouseover-entity :as mouseover-entity]
            [cdq.context.ui.player-message :as player-message]
            [cdq.context.world :as world]
            [cdq.context :refer [rebuild-inventory-widgets reset-actionbar]]))

(defn start-game-context [context]
  (rebuild-inventory-widgets context)
  (reset-actionbar context)
  (let [context (merge context
                       (ecs/->context :z-orders [:z-order/on-ground
                                                 :z-order/ground
                                                 :z-order/flying
                                                 :z-order/effect])
                       (mouseover-entity/->context)
                       (player-message/->context)
                       (counter/->context)
                       {:context/game-paused? (atom true)
                        :context/game-logic-frame (atom 0)})]
    (world/merge->context context)))
