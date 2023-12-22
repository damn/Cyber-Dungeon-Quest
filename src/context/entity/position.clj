(ns context.entity.position
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as entity]
            [game.context :refer [content-grid]]
            [game.world.content-grid :refer [update-entity! remove-entity!]]))

(defcomponent :position _
  (entity/create!  [_ e ctx]      (update-entity! (content-grid ctx) e))
  (entity/destroy! [_ e ctx]      (remove-entity! (content-grid ctx) e))
  (entity/moved!   [_ e ctx _dir] (update-entity! (content-grid ctx) e)))
