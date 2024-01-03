(ns context.entity.delete-after-duration
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as ecs]
            [cdq.context :refer [->counter stopped?]]))

(defcomponent :entity/delete-after-duration counter
  (ecs/create! [[k duration] entity context]
    (swap! entity assoc k (->counter context duration)))
  (ecs/tick! [_ entity ctx]
    (when (stopped? ctx counter)
      (swap! entity assoc :entity/destroyed? true))))
