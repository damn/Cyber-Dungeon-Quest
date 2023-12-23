(ns context.entity.position
  (:require [x.x :refer [defcomponent]]
            [context.entity :as entity]
            [cdq.context :refer [content-grid]]
            [cdq.world.content-grid :refer [update-entity! remove-entity!]]))

(defcomponent :position _
  (entity/create!  [_ e ctx]      (update-entity! (content-grid ctx) e))
  (entity/destroy! [_ e ctx]      (remove-entity! (content-grid ctx) e))
  (entity/moved!   [_ e ctx _dir] (update-entity! (content-grid ctx) e)))
