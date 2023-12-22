(ns context.entity.delete-after-duration
  (:require [x.x :refer [defcomponent]]
            [data.counter :as counter]
            [context.entity :as entity]))

(defcomponent :delete-after-duration counter
  (entity/create [[_ duration]]
    (counter/create duration))
  (entity/tick [_ delta]
    (counter/tick counter delta))
  (entity/tick! [_ entity _ctx delta]
    (when (counter/stopped? counter)
      (swap! entity assoc :destroyed? true))))
