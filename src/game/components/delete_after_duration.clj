(nsx game.components.delete-after-duration
  (:require [game.utils.counter :as counter]))

(defcomponent :delete-after-duration duration
  (create [_]
    (counter/make-counter duration))
  (tick! [[k _] e delta]
    (when (counter/update-counter! e delta [k])
      (swap! e assoc :destroyed? true))))
