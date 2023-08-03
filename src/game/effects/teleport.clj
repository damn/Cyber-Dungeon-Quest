
#_(defn- teleport-animation [posi]
  (animation-entity
    :animation (create-animation (spritesheet-frames "effects/blue_teleport.png" 17 17)
                                 :frame-duration 100)
    :position posi))

(comment
  (deflearnable-skill teleport
    :cost 15
    :info "Teleports you to the target destination."
    {:check-usable (fn [entity _]
                     (valid-position? (g/map-coords)
                                      entity))
     :do-skill (fn [entity component]
                 (audio/play "bfxr_playerteleport.wav")
                 (teleport-animation (get-skill-use-mouse-tile-pos))
                 (teleport entity (get-skill-use-mouse-tile-pos)))}))


