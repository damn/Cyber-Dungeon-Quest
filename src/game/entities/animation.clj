(ns game.entities.animation
  (:require [game.db :as db]))

(defn create! [& {:keys [position animation]}]
  (db/create-entity!
   {:position position
    :animation animation
    :z-order :effect
    :delete-after-animation-stopped? true}))
