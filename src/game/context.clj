(ns game.context)

(defprotocol EntityComponentSystem
  (create-entity! [_ components-map]
                  "Entities should not have :id component, will get added.
                  Calls create/create! system on the components-map
                  Then puts it into an atom and calls entity/create! system on all components.")
  (get-entity [_ id])
  (tick-entity [_ entity delta] "Calls entity/tick on all components and then entity/tick!")
  (render-entities* [_ entities*] "In the correct z-order and in the order of render-systems for each z-order.")
  (remove-destroyed-entities [_] "Calls entity/destroy and entity/destroy! on all entities which are marked as ':destroyed?'"))

(defprotocol PlayerMessage
  (show-msg-to-player! [_ message])
  (->player-message-actor [_]))

(defprotocol MouseOverEntity
  (update-mouseover-entity [_]))

(defprotocol World
  (line-of-sight? [_ source* target*])
  (ray-blocked?  [_ start target])
  (path-blocked? [_ start target path-w] "path-w in tiles. casts two rays.")
  ; TODO explored-grid
  (explored?     [_ position])
  (set-explored! [_ position])
  (content-grid [_])
  (get-active-entities [_])
  (world-grid [_]))

(defprotocol EffectInterpreter
  (do-effect!         [_ effect])
  (effect-text        [_ effect])
  (valid-params?      [_ effect])
  (effect-render-info [_ effect])
  (effect-useful?     [_ effect]))

(defprotocol Builder
  (creature-entity [_ creature-id position creature-params])
  (audiovisual [_ position property-id])
  (item-entity [_ position item])
  (line-entity [_ {:keys [start end duration color thick?]}])
  (projectile-entity [_ {:keys [position
                                faction
                                size
                                animation
                                movement-vector
                                hit-effect
                                speed
                                maxtime
                                piercing]}]))

; TODO get from world?
(defprotocol PotentialField
  (update-potential-fields [_ entities])
  (potential-field-follow-to-enemy [_ entity]))

(defprotocol FiniteStateMachine
  (send-event! [_ entity event]
               [_ entity event params]))

; TODO add update-and-write and move to game.context
(defprotocol PropertyStore
  (get-property [_ id])
  (all-properties [_ type]))
