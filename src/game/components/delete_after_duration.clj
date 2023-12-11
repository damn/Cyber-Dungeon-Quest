(ns game.components.delete-after-duration
  (:require [x.x :refer [defcomponent]]
            [game.entity :as entity]
            [game.utils.counter :as counter]))

(defcomponent :delete-after-duration counter
  (entity/create [[_ duration]]
    (counter/create duration))
  (entity/tick [_ delta]
    (counter/tick counter delta))
  (entity/tick! [_ _ctx entity delta]
    (when (counter/stopped? counter)
      (swap! entity assoc :destroyed? true))))
