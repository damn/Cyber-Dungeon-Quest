(ns game.components.position
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as entity]
            [context.world :refer (put-entity-in-correct-content-field remove-entity-from-content-field)]))

(defcomponent :position _
  (entity/create! [_ e context]
    (put-entity-in-correct-content-field context e))
  (entity/destroy! [_ e _ctx]
    (remove-entity-from-content-field e))
  (entity/moved! [_ e context direction-vector] ; TODO needs context, also call it position-changed!
    (put-entity-in-correct-content-field context e)))
