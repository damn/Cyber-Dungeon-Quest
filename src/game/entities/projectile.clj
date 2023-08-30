(ns game.entities.projectile
  (:require [gdl.audio :as audio]
            [game.db :as db]
            [game.media :as media]
            game.components.body.rotation-angle
            game.components.delete-after-duration
            [game.entities.animation :as animation-entity]))

(defn- hit-wall-effect [position]
  (audio/play "bfxr_projectile_wallhit.wav")
  (animation-entity/create!
   :position position
   :animation (media/plop-animation)))

; TODO maxrange ?
; TODO make only common fields here
(defn create!
  [& {:keys [position
             faction
             size
             animation
             movement-vector
             hit-effects
             speed
             maxtime
             piercing]}]
  (db/create-entity!
   {:position position
    :faction faction
    :body {:width size
           :height size
           :is-solid false
           :rotation-angle 0}
    :z-order :effect
    :speed speed
    :movement-vector movement-vector
    :animation animation
    :delete-after-duration maxtime
    :projectile-collision {:piercing piercing
                           :hit-effects hit-effects
                           :already-hit-bodies #{}
                           :hits-wall-effect hit-wall-effect}}))
