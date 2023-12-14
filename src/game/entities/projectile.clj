(ns game.entities.projectile
  (:require [game.db :as db]))

; TODO maxrange ?
; TODO make only common fields here
(defn create!
  [{:keys [position
           faction
           size
           animation
           movement-vector
           hit-effects
           speed
           maxtime
           piercing]}
   context]
  (db/create-entity! context
                     {:position position
                      :faction faction
                      :body {:width size
                             :height size
                             :is-solid false
                             :rotation-angle 0
                             :rotate-in-movement-direction? true}
                      :z-order :effect
                      :speed speed
                      :movement-vector movement-vector
                      :animation animation
                      :delete-after-duration maxtime
                      :projectile-collision {:piercing piercing
                                             :hit-effects hit-effects
                                             :already-hit-bodies #{}}}))
