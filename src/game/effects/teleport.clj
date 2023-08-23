
#_(defn- teleport-animation [posi]
    (audio/play "bfxr_playerteleport.wav")
    (animation-entity/create!
     :animation (create-animation (spritesheet-frames "effects/blue_teleport.png" 17 17)
                                  :frame-duration 100)
     :position posi))

(comment
  (deflearnable-skill teleport
    :cost 15
    :info "Teleports you to the target destination."
    {:check-usable (fn [entity _]
                     (valid-position? (world/mouse-position)
                                      entity))
     :do-skill (fn [entity component]
                 (teleport-animation (get-skill-use-mouse-tile-pos))
                 (teleport entity (get-skill-use-mouse-tile-pos)))}))


