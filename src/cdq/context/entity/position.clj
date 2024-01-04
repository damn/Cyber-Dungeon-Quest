(ns cdq.context.entity.position
  (:require [x.x :refer [defcomponent]]
            [cdq.context.ecs :as ecs]
            [cdq.context :refer [content-grid]]
            [cdq.world.content-grid :refer [update-entity! remove-entity!]]))

(defcomponent :entity/position _
  (ecs/create!  [_ entity ctx] (update-entity! (content-grid ctx) entity))
  (ecs/destroy! [_ entity ctx] (remove-entity! (content-grid ctx) entity)))
