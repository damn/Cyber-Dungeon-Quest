(ns game.components.id
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as entity]))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defcomponent :id id
  (entity/create [_]
    (assert (nil? id))
    (unique-number!))
  (entity/create! [_ e {:keys [context/ids->entities]}]
    (swap! ids->entities assoc id e))
  (entity/destroy! [_ e {:keys [context/ids->entities]}]
    (swap! ids->entities dissoc id)))
