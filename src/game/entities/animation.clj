(ns game.entities.animation
  (:require [game.db :as db]
            game.components.delete-after-animation-stopped?))

(defn create! [& {:keys [position animation]}]
  (db/create-entity!
   {:position position
    :animation animation
    :z-order :effect
    :delete-after-animation-stopped? true}))
