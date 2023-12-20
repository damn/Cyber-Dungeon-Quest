(ns game.context)

(defprotocol EntityComponentSystem
  (get-entity [_ id])
  ; TODO remove !
  (entity-exists? [_ entity])
  (create-entity! [_ components-map]
                  "Entities should not have :id component, will get added.
                  Calls entity/create system on the components-map
                  Then puts it into an atom and calls entity/create! system on all components.")

  ; TODO pass entities, just tick!
  (tick-active-entities [_ delta])
  ; TODO pass entities & check visible before, just render !
  (render-visible-entities [_])

  ; TODO after tick do
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
  (update-potential-fields [_])
  (potential-field-follow-to-enemy [_ entity]))

(defprotocol FiniteStateMachine
  (send-event! [_ entity event]
               [_ entity event params]))
