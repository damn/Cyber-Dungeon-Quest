(ns game.components.delete-after-duration
  (:require [x.x :refer [defcomponent]]
            [game.db :as db]
            [game.tick :refer [tick!]]
            [game.utils.counter :as counter]))

(defcomponent :delete-after-duration duration
  (db/create [_]
    (counter/make-counter duration))
  (tick! [[k _] e delta]
    (when (counter/update-counter! e delta [k])
      (swap! e assoc :destroyed? true))))
