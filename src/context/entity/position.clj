(ns context.entity.position
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as ecs]
            [cdq.context :refer [content-grid]]
            [cdq.world.content-grid :refer [update-entity! remove-entity!]]))

(defcomponent :entity/position _
  (ecs/create!  [_ e ctx]      (update-entity! (content-grid ctx) e))
  (ecs/destroy! [_ e ctx]      (remove-entity! (content-grid ctx) e))
  (ecs/moved!   [_ e ctx _dir] (update-entity! (content-grid ctx) e)))
