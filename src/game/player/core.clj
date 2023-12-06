(ns game.player.core
  (:require [gdl.audio :as audio]
            game.maps.data
            [game.running :refer (running)]
            [game.components.skills :refer (reset-cooldowns)]
            [game.player.entity :refer :all]
            [game.utils.msg-to-player :refer (show-msg-to-player)]))

; TODO player death & revive -> just create a new player entity ??
; TODO new session -> reset running state. (its paused)

(defn player-death
  "this is NOT implemented as a death-trigger (immediately called at deal-dmg and hp<0) because the snapshot and update order of components
  affected the player-entity after the death event ->  the rotation-angle of player-entity was altered in rare cases.
  To be independent of the order of entitiy updates call this after the frame is finished updating.
  TLDR: player components affected player-entity after deathtrigger happened."
  ; TODO but then the same thing can happen with monsters?
  ; death-trigger happens and still other components get updated
  ; -> should be triggered at update-removelist?
  ; but is it not already ?
  []
  #_(show-msg-to-player (str "You died!\nPress ESCAPE to "
                           (let [restorations (get-inventory-cells-with-item-name "Restoration" :inventory)]
                             (if (seq restorations)
                               (str "be revived. Restorations left: " (dec (count restorations)) "")
                               "exit the game."))))
  (let [entity player-entity]
    (reset! running false) ; TODO remove references to 'running' into update-ingame not here.
    (audio/play "sounds/bfxr_playerdeath.wav")
    ;(remove-body-effects player-entity) not implemented
    #_(assoc-in! player-entity [:hp :current] 0)))


; TODO ctype-fns :on-revive -> restore everything to initial state?
#_(defn- revive-player []
  (show-msg-to-player "") ; removes the old msg
  (reset! running true)
  (teleport player-entity (:start-position (game.maps.data/get-current-map-data)))
  (->! player-entity
       ; TODO defctypefn reset-state ?
       ; but basically we are creating a new player ?
       (assoc :is-dead false)
       (update :hp   set-to-max)
       (update :mana set-to-max)
       ; (switch-state :skillmanager :ready) ; TODO reset state
       (update :skills reset-cooldowns)))

#_(defn try-revive-player []
  ; TODO make abstraction 'remove-from-inventory item-name'
  #_(when-seq [cells (get-inventory-cells-with-item-name "Restoration" :inventory)]
    (remove-item-from-cell (rand-nth cells))
    (revive-player)
    true))

