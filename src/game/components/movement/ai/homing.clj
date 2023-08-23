(nsx game.components.movement.ai.homing)

#_(defn- move-and-rotate-to-target-control
  [projectile {:keys [target-body current-angle rotationspeed]} delta]
  (v/vector-from-angle
   (if-not (db/exists? target-body)
     current-angle
     (let [angle-to-target (v/get-angle-to-position (:position @projectile)
                                                    (:position @target-body))
           adjusted-angle (v/rotate-angle-to-angle current-angle
                                                   angle-to-target
                                                   rotationspeed
                                                   delta)]
       (assoc-in! projectile [:movement :current-angle] adjusted-angle)
       adjusted-angle))))

#_(defn create-homing-movement [target-body start-angle rotationspeed]
  {:movement {:control-update move-and-rotate-to-target-control
              :target-body target-body
              :current-angle start-angle
              :rotationspeed rotationspeed}})

#_(defctypefn :update-entity* :player-movement [entity* delta]
  (assoc entity*
         :movement-vector
         (if (:active-skill? entity*)
           nil
           (calc-movement-v entity*))))
