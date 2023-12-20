(ns game.context)

(defprotocol EntityComponentSystem
  (get-entity [_ id])
  (entity-exists? [_ entity])
  (create-entity! [_ components-map]
                  "Entities should not have :id component, will get added.
                  Calls entity/create system on the components-map
                  Then puts it into an atom and calls entity/create! system on all components.")
  (tick-active-entities [_ delta])
  (render-visible-entities [_])
  (destroy-to-be-removed-entities! [_]
                                   "Calls entity/destroy and entity/destroy! on all entities which are marked as ':destroyed?'"))

(defprotocol PlayerMessage
  (show-msg-to-player! [_ message]))

(defprotocol MouseOverEntity
  (update-mouseover-entity [_]))

(defprotocol World
  (line-of-sight? [_ source* target*])
  (ray-blocked?  [_ start target])
  (path-blocked? [_ start target path-w] "path-w in tiles. casts two rays.")
  (explored?     [_ position])
  (set-explored! [_ position])
  (content-grid [_])
  (get-active-entities [_])
  (world-grid [_]))

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
                                hit-effects
                                speed
                                maxtime
                                piercing]}]))

(defprotocol PotentialField
  (update-potential-fields [_])
  (potential-field-follow-to-enemy [_ entity]))

(defprotocol FiniteStateMachine
  (send-event! [_ entity event]
               [_ entity event params]))
