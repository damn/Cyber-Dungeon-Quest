(ns cdq.context)

; TODO cdq.transaction
(defmulti transact! (fn [[k] _ctx] k))

(defprotocol TransactionHandler
  (transact-all! [_ txs]))

(defprotocol EntityComponentSystem
  (get-entity [_ id])
  (tick-entity! [_ entity] "Calls tick on all components of the entity.")
  (render-entities* [_ entities*] "Draws entities* in the correct z-order and in the order of render-systems for each z-order.")
  (remove-destroyed-entities! [_] "Calls destroy on all entities which are marked as ':entity/destroyed?'"))

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
  ; TODO remove
  (apply-modifier!   [_ entity modifier])
  (reverse-modifier! [_ entity modifier])
  (modifier-text     [_ modifier]))

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
  (rebuild-inventory-widgets [_])
  ; TODO trigger
  (set-item-image-in-widget [_ cell item])
  (remove-item-from-widget [_ cell]))

(defprotocol Inventory
  (try-pickup-item! [_ entity item])) ; TODO remove

(defprotocol Counter
  (->counter [_ duration])
  (stopped?       [_ counter])
  (reset          [_ counter])
  (finished-ratio [_ counter])
  (update-elapsed-game-time! [_]))

(defprotocol Skills ; TODO remove
  (add-skill! [_ entity skill])
  (remove-skill! [_ entity skill])
  (skill-usable-state [effect-context entity* skill]))

(defprotocol Actionbar
  (->action-bar    [_])
  (reset-actionbar [_])
  (selected-skill  [_])
  ; TODO trigger
  (actionbar-add-skill    [_ skill])
  (actionbar-remove-skill [_ skill]))

(defprotocol DebugRender
  (debug-render-before-entities [_])
  (debug-render-after-entities  [_]))

(defprotocol Cursor
  (set-cursor! [_ cursor-key]))

(defprotocol TooltipText
  (tooltip-text [_ property]))

(defprotocol ErrorModal
  (->error-window [_ throwable]))
