(ns cdq.entity.position
  (:require [x.x :refer [defcomponent]]
            [gdl.math.vector :as v]
            [utils.core :refer [->tile]]
            [cdq.api.entity :as entity]))

(extend-type cdq.api.entity.Entity
  entity/Position
  (tile [{:keys [entity/position]}]
    (->tile position))

  (direction [{:keys [entity/position]} other-entity*]
    (v/direction position (:entity/position other-entity*))))

(defcomponent :entity/position {}
  _
  (entity/create [_ {:keys [entity/id]} ctx]
    [[:tx/add-to-world id]])

  (entity/destroy [_ {:keys [entity/id]} ctx]
    [[:tx/remove-from-world id]]))
