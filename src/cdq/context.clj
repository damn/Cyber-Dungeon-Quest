(ns cdq.context)

(defmulti transact!
  (fn [{tx-key 0 :as transaction} context]
    tx-key))

(comment
 (clojure.pprint/pprint (sort (keys (methods transact!))))
 #_(; # entity
    ; ## ecs core
    :tx/create
    :tx/destroy
    :tx/assoc
    :tx/assoc-in
    :tx/dissoc
    :tx/dissoc-in
    ; ## skills
    :tx/add-skill
    :tx/remove-skill
    :tx/add-text-effect
    :tx/apply-modifier
    :tx/reverse-modifier
    ; ## state
    :tx/event
    :tx/position-changed
    ; ## inventory
    :tx/set-item
    :tx/remove-item
    :tx/pickup-item
    :tx/stack-item
    ; # builders
    :tx/audiovisual
    :tx/creature
    ; # other
    :tx/cursor
    :tx/sound
    :tx/effect
    ; # ui
    :tx/msg-to-player
    :tx/player-modal
    ; ## inventory
    :tx/set-item-image-in-widget
    :tx/remove-item-from-widget
    ; ## actionbar
    :tx/actionbar-add-skill
    :tx/actionbar-remove-skill
    )
 )

(defprotocol TransactionHandler
  (transact-all! [_ txs]))

(defprotocol EntityComponentSystem
  (get-entity [_ id])
  (tick-entity! [_ entity] "Calls tick on all components of the entity.")
  (render-entities* [_ entities*] "Draws entities* in the correct z-order and in the order of render-systems for each z-order.")
  (remove-destroyed-entities! [_] "Calls destroy on all entities which are marked with ':tx/destroy'"))

(defprotocol MouseOverEntity
  (update-mouseover-entity! [_]))

(defprotocol World
  (line-of-sight? [_ source* target*])
  (ray-blocked?  [_ start target])
  (path-blocked? [_ start target path-w] "path-w in tiles. casts two rays.")
  ; TODO explored-grid
  (explored?     [_ position])
  (set-explored! [_ position])
  (content-grid [_])
  (world-grid [_]))

(defprotocol EntityWorld
  (add-entity! [_ entity])
  (remove-entity! [_ entity]))

(defprotocol EffectInterpreter
  (effect-text        [_ effect])
  (valid-params?      [_ effect])
  (effect-render-info [_ effect])
  (effect-useful?     [_ effect]))

(defprotocol Modifier
  (modifier-text [_ modifier]))

(defprotocol Builder
  ; TODO ?
  (item-entity [_ position item])
  (line-entity [_ {:keys [start end duration color thick?]}]))

; TODO get from world?
(defprotocol PotentialField
  (update-potential-fields! [_ entities])
  (potential-field-follow-to-enemy [_ entity]))

(defprotocol PropertyStore
  (get-property [_ id])
  (all-properties [_ type]))

(defprotocol InventoryWindow
  (inventory-window [_])
  (rebuild-inventory-widgets [_]))

(defprotocol Counter
  (->counter [_ duration])
  (stopped?       [_ counter])
  (reset          [_ counter])
  (finished-ratio [_ counter])
  (update-elapsed-game-time! [_]))

(defprotocol Skills
  (skill-usable-state [effect-context entity* skill]))

(defprotocol Actionbar
  (->action-bar    [_])
  (reset-actionbar [_])
  (selected-skill  [_]))

(defprotocol DebugRender
  (debug-render-before-entities [_])
  (debug-render-after-entities  [_]))

(defprotocol Cursor
  (set-cursor! [_ cursor-key]))

(defprotocol TooltipText
  (tooltip-text [_ property])
  (player-tooltip-text [_ property]))

(defprotocol ErrorModal
  (->error-window [_ throwable]))
