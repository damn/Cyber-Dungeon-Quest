(ns game.components.delete-after-duration
  (:require [x.x :refer [defcomponent]]
            [game.db :as db]
            [game.tick :refer [tick tick!]]
            [game.utils.counter :as counter]))

(defcomponent :delete-after-duration counter
  (db/create [[_ duration]]
    (counter/create duration))
  (tick [_ delta]
    (counter/tick counter delta))
  (tick! [_ entity delta]
    (when (counter/stopped? counter)
      (swap! entity assoc :destroyed? true))))
