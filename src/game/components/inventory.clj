(ns game.components.inventory
  (:require [data.grid2d :as grid]
            [x.x :refer [defcomponent]]
            [utils.core :refer [assoc-in! find-first]]
            [game.db :as db]
            [game.properties :as properties]
            [game.components.modifiers :as modifiers]))

; skills added/removed => make also gui-callback fn and bind-root.

(def empty-inventory
  (->> {:bag      [6 4]
        :weapon   [1 1]
        :shield   [1 1]
        :helm     [1 1]
        :chest    [1 1]
        :leg      [1 1]
        :glove    [1 1]
        :boot     [1 1]
        :cloak    [1 1]
        :necklace [1 1]
        :rings    [2 1]}
       (map (fn [[slot [width height]]]
              [slot
               (grid/create-grid width height (constantly nil))]))
       (into {})))

(defn- cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn- slot->items [inventory slot]
  (-> inventory slot grid/cells))

(defn- cell-exists? [inventory [slot position]]
  (-> inventory slot (contains? position)))

(defn valid-slot? [[slot _] item]
  (or (= :bag slot)
      (= (:slot item) slot)))

(defn two-handed-weapon-and-shield-together? [inventory {slot 0 :as cell} new-item]
  (or (and (:two-handed? new-item)
           (= slot :weapon)
           (first (slot->items inventory :shield)))
      (and (= (:slot new-item) :shield)
           (= slot :shield)
           (if-let [weapon (first (slot->items inventory :weapon))]
             (:two-handed? weapon)))))

(defn- assoc-inventory-cell! [entity cell item]
  (assoc-in! entity (cons :inventory cell) item))

(defn- applies-modifiers? [[slot _]]
  (not= :bag slot))

; call gui-callback and bind-root & add :is-player check
(declare set-item-image-in-widget
         remove-item-from-widget)

; TODO make entity* / functional ?? move side effects outside !!

(defn set-item [entity cell item]
  {:pre [(nil? (get-in (:inventory @entity) cell))
         (valid-slot? cell item)
         (not (two-handed-weapon-and-shield-together? (:inventory @entity) cell item))]}
  (when (:is-player @entity)
    (set-item-image-in-widget cell item))
  (assoc-inventory-cell! entity cell item)
  (when (applies-modifiers? cell)
    (modifiers/apply! entity (:modifiers item))))

(defn remove-item [entity cell]
  (let [item (get-in (:inventory @entity) cell)]
    (assert item)
    (when (:is-player @entity)
      (remove-item-from-widget cell))
    (assoc-inventory-cell! entity cell nil)
    (when (applies-modifiers? cell)
      (modifiers/reverse! entity (:modifiers item)))))

(defn cell-in-use? [entity* cell] ; TODO naming (includes is-active check) ->
  (let [inventory (:inventory entity*)
        item (get-in inventory cell)]
    (and item
         (applies-modifiers? cell)
         (:active-skill? entity*))))

; TODO also move in inventory ? 'item-on-cursor' -> gets saved/loaded with entitydata and reset.
; but keys inventory have to correspond to slots -> at some code.
(def item-in-hand (atom nil)) ; TODO -on-cursor not in hand
; TODO state for this ??

(defn is-item-in-hand? []
  @item-in-hand)

(defn set-item-in-hand [item]
  (reset! item-in-hand item))

(defn empty-item-in-hand []
  (reset! item-in-hand nil))

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; TODO this is not required but can be asserted, all of one name should have count if others have count
       (= (:id item-a) (:id item-b))))

(defn stack-item [entity cell item]
  (let [cell-item (get-in (:inventory @entity) cell)]
    (assert (stackable? item cell-item))
    (remove-item entity cell)
    (set-item entity cell (update cell-item :count + (:count item)))))

(defn remove-one-item [entity cell]
  (let [item (get-in (:inventory @entity) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       (remove-item entity cell)
       (set-item entity cell (update item :count dec)))
      (remove-item entity cell))))

(defn- try-put-item-in
  "returns true when the item was picked up"
  [entity slot item]
  (let [inventory (:inventory @entity)
        cells-items (cells-and-items inventory slot)

        [cell cell-item] (find-first (fn [[cell cell-item]] (stackable? item cell-item))
                                cells-items)
        picked-up (if cell
                    (do (stack-item entity cell item)
                        true)
                    (when-let [[empty-cell] (find-first (fn [[cell item]] (nil? item))
                                                     cells-items)]
                      (when-not (two-handed-weapon-and-shield-together? inventory empty-cell item)
                        (set-item entity empty-cell item)
                        true)))]
    picked-up))

(defn try-pickup-item [entity item]
  (let [slot (find-first #(= % (:slot item))
                         (keys (:inventory @entity)))]
    (or
     (try-put-item-in entity slot item)
     (try-put-item-in entity :bag item))))

; after-create-entity because try-pickup-item applies modifiers (skillmanager has to be initialised)
(defcomponent :items items
  (db/after-create! [_ entity]
    (swap! entity assoc :inventory empty-inventory)
    ;(swap! entity dissoc :items)
    (doseq [id items]
      (try-pickup-item entity (properties/get id)))))