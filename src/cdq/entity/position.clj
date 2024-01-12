(ns cdq.entity.position
  (:require [gdl.math.vector :as v]
            [utils.core :refer [->tile]]
            [cdq.entity :as entity]))

(extend-type cdq.entity.Entity
  entity/Position
  (tile [{:keys [entity/position]}]
    (->tile position))

  (direction [{:keys [entity/position]} other-entity*]
    (v/direction position (:entity/position other-entity*))))
