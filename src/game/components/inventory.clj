(ns game.components.inventory
  (:require [data.grid2d :as grid]
            [x.x :refer [defcomponent]]
            [utils.core :refer [safe-get find-first]]
            [game.entity :as entity]
            [game.modifier :as modifier]
            [game.components.skills :as skills]))

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

(defn applies-modifiers? [[slot _]]
  (not= :bag slot))

(defn- inv-set-item [inventory cell item]
  {:pre [(nil? (get-in inventory cell))
         (valid-slot? cell item)
         (not (two-handed-weapon-and-shield-together? inventory cell item))]}
  (assoc-in inventory cell item))

(defn- inv-remove-item [inventory cell]
  {:pre [(get-in inventory cell)]}
  (assoc-in inventory cell nil))

(declare set-item-image-in-widget!
         remove-item-from-widget!)

(defn set-item! [entity cell item]
  (swap! entity update :inventory inv-set-item cell item)
  (when (applies-modifiers? cell)
    (swap! entity modifier/apply-modifiers (:modifiers item))
    (when (and (= (:slot item) :weapon))
      (swap! entity update :skills skills/add-skill item)))
  (when (:is-player @entity)
    (set-item-image-in-widget! cell item)))

(defn remove-item! [entity cell]
  (let [item (get-in (:inventory @entity) cell)]
    (swap! entity update :inventory inv-remove-item cell)
    (when (applies-modifiers? cell)
      (swap! entity modifier/reverse-modifiers (:modifiers item))
      (when (= (:slot item) :weapon)
        (swap! entity update :skills skills/remove-skill item)))
    (when (:is-player @entity)
      (remove-item-from-widget! cell))))

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; TODO this is not required but can be asserted, all of one name should have count if others have count
       (= (:id item-a) (:id item-b))))

; TODO no items which stack are available
(defn stack-item! [entity cell item]
  (let [cell-item (get-in (:inventory @entity) cell)]
    (assert (stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (remove-item! entity cell)
    (set-item! entity cell (update cell-item :count + (:count item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [entity cell]
  (let [item (get-in (:inventory @entity) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! entity cell)
       (set-item! entity cell (update item :count dec)))
      (remove-item! entity cell))))

(defn- try-put-item-in!
  "returns true when the item was picked up"
  [entity slot item]
  (let [inventory (:inventory @entity)
        cells-items (cells-and-items inventory slot)
        [cell cell-item] (find-first (fn [[cell cell-item]] (stackable? item cell-item))
                                     cells-items)
        picked-up (if cell
                    (do (stack-item! entity cell item)
                        true)
                    (when-let [[empty-cell] (find-first (fn [[cell item]] (nil? item))
                                                        cells-items)]
                      (when-not (two-handed-weapon-and-shield-together? inventory empty-cell item)
                        (set-item! entity empty-cell item)
                        true)))]
    picked-up))

(defn try-pickup-item! [entity item]
  (let [slot (find-first #(= % (:slot item))
                         (keys (:inventory @entity)))]
    (or
     (try-put-item-in! entity slot item)
     (try-put-item-in! entity :bag item))))

(defcomponent :items items
  (entity/create! [_ entity {:keys [context/properties]}]
    (swap! entity assoc :inventory empty-inventory)
    ;(swap! entity dissoc :items)
    (doseq [id items]
      (try-pickup-item! entity (safe-get properties id)))))
