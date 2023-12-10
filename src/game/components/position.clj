(ns game.components.position
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [int-posi]]
            [game.entity :as entity]
            [game.maps.contentfields :refer (put-entity-in-correct-content-field
                                             remove-entity-from-content-field)]))

(defcomponent :position _
  (entity/create! [_ e]
    (put-entity-in-correct-content-field e))
  (entity/destroy! [_ e]
    (remove-entity-from-content-field e))
  (entity/moved! [_ e direction-vector]
    (put-entity-in-correct-content-field e)))

(def get-tile (comp int-posi :position))
