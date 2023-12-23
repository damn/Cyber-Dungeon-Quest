(ns context.entity.inventory
  (:require [data.grid2d :as grid]
            [x.x :refer [defcomponent]]
            gdl.context
            [utils.core :refer [find-first]]
            [context.entity :as entity]
            [cdq.context :refer [get-property set-item-image-in-widget remove-item-from-widget apply-modifier! reverse-modifier! try-pickup-item! remove-item! set-item! stack-item! add-skill! remove-skill!]]
            cdq.entity))

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
               (grid/create-grid width height (constantly nil))])) ; simple hashmap grid?
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

(defn- set-item [entity* cell item]
  (update entity* :inventory inv-set-item cell item))

(defn- remove-item [entity* cell]
  (update entity* :inventory inv-remove-item cell))

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; TODO this is not required but can be asserted, all of one name should have count if others have count
       (= (:id item-a) (:id item-b))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [entity cell]
  (let [item (get-in (:inventory @entity) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! context entity cell)
       (set-item! context entity cell (update item :count dec)))
      (remove-item! context entity cell))))

(defn- try-put-item-in!
  "returns true when the item was picked up"
  [context entity slot item]
  (let [inventory (:inventory @entity)
        cells-items (cells-and-items inventory slot)
        [cell cell-item] (find-first (fn [[cell cell-item]] (stackable? item cell-item))
                                     cells-items)
        picked-up (if cell
                    (do (stack-item! context entity cell item)
                        true)
                    (when-let [[empty-cell] (find-first (fn [[cell item]] (nil? item))
                                                        cells-items)]
                      (when-not (two-handed-weapon-and-shield-together? inventory empty-cell item)
                        (set-item! context entity empty-cell item)
                        true)))]
    picked-up))

(extend-type gdl.context.Context
  cdq.context/Inventory
  (set-item! [context entity cell item]
    (swap! entity set-item cell item)

    (when (applies-modifiers? cell)
      (apply-modifier! context entity (:modifier item))
      (when (and (= (:slot item) :weapon))
        (add-skill! context entity item)))

    (when (:is-player @entity)
      (set-item-image-in-widget context cell item)))

  (remove-item! [context entity cell]
    (let [item (get-in (:inventory @entity) cell)]
      (swap! entity remove-item cell)

      (when (applies-modifiers? cell)
        (reverse-modifier! context entity (:modifier item))
        (when (= (:slot item) :weapon)
          (remove-skill! context entity item)))

      (when (:is-player @entity)
        (remove-item-from-widget context cell))))

  ; TODO no items which stack are available
  (stack-item! [context entity cell item]
    (let [cell-item (get-in (:inventory @entity) cell)]
      (assert (stackable? item cell-item))
      ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
      ; first remove and then place, just update directly  item ...
      (remove-item! context entity cell)
      (set-item! context entity cell (update cell-item :count + (:count item)))))

  (try-pickup-item! [context entity item]
    (let [slot (find-first #(= % (:slot item))
                           (keys (:inventory @entity)))]
      (or
       (try-put-item-in! context entity slot item)
       (try-put-item-in! context entity :bag item)))))

(defcomponent :items items
  (entity/create! [_ entity context]
    (swap! entity assoc :inventory empty-inventory)
    ;(swap! entity dissoc :items)
    (doseq [id items]
      (try-pickup-item! context entity (get-property context id)))))
