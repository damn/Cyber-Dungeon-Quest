(ns game.entities.animation)

(defn create [& {:keys [position animation]}]
  {:position position
   :animation animation
   :z-order :effect
   :delete-after-animation-stopped? true})
