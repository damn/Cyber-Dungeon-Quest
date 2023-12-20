(ns game.components.position
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as entity]
            [game.context :refer [update-entity! remove-entity!]]
            [game.world.content-grid :as content-grid]))

(defcomponent :position _
  (entity/create!  [_ e ctx]      (update-entity! (content-grid ctx) e))
  (entity/destroy! [_ e ctx]      (remove-entity! (content-grid ctx) e))
  (entity/moved!   [_ e ctx _dir] (update-entity! (content-grid ctx) e)))
