(ns context.properties
  (:require [clojure.edn :as edn]
            [gdl.context :refer [get-sprite]]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get]]
            cdq.context))

; TODO move all property-type , attribute, etc. config together
; sort-order, overview-title, etc. everything

(comment

 (do
  (require '[malli.provider :as mp])
  (set! *print-level* nil)
  (let [ctx @gdl.app/current-context
        properties (:context/properties ctx)
        creatures (cdq.context/all-properties ctx :property.type/creature)]
    (clojure.pprint/pprint
     (mp/provide (map #(dissoc % :property/image) creatures)))))
 ; =>
 [:map
  [:property/id :keyword] ; namespaced creature/vampire
  [:creature/species :qualified-keyword] ; one of species (or move in creatures)
  [:creature/skills [:set :qualified-keyword]] ; one of SPELLS ids, not skills (passives?)
  [:creature/items [:set :keyword]] ; one of items ...
  [:creature/level [:maybe :int]]] ; ?

 )

(defn- default-property-tooltip-text [context props]
  (binding [*print-level* nil]
    (with-out-str
     (clojure.pprint/pprint (dissoc props :property/image)))))

; fn for checking if = property-type
; weapons are both item & weapons ( & skills, not spells)
(def property-types
  {:property.type/skill {:of-type? (fn [{:keys [item/slot skill/effect]}]
                                     (and (not slot) effect))
                         :sort-order 0
                         :title "Spell"
                         :overview {:title "Spells"
                                    :columns 16
                                    :image/dimensions [70 70]
                                    :tooltip-text-fn (fn [ctx props]
                                                       (try (cdq.context/skill-text ctx props)
                                                            (catch Throwable t
                                                              (default-property-tooltip-text ctx props))))}}
   :property.type/creature {:of-type? :creature/species
                            :sort-order 1
                            :title "Creature"
                            :overview {:title "Creatures"
                                       :columns 16
                                       :image/dimensions [65 65]
                                       :sort-by-fn #(vector (or (:creature/level %) 9)
                                                            (name (:creature/species %))
                                                            (name (:property/id %)))
                                       :extra-info-text #(or (str (:creature/level %)) "-")
                                       :tooltip-text-fn default-property-tooltip-text}}
   :property.type/species {:of-type? :creature/hp
                           :sort-order 2
                           :title "Species"
                           :overview {:title "Species"
                                      :columns 2
                                      :tooltip-text-fn default-property-tooltip-text}}
   :property.type/item {:of-type? :item/slot
                        :sort-order 3
                        :title "Item"
                        :overview {:title "Items"
                                   :columns 17
                                   :image/dimensions [60 60]
                                   :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                          (name slot)
                                                          "")
                                                        (name (:property/id %)))
                                   :tooltip-text-fn default-property-tooltip-text}}
   :property.type/weapon {:of-type? (fn [{:keys [item/slot]}]
                                      (and slot (= slot :inventory.slot/weapon)))
                          :sort-order 4
                          :title "Weapon"
                          :overview {:title "Weapons"
                                     :columns 10
                                     :image/dimensions [96 96]
                                     :tooltip-text-fn (fn [ctx props]
                                                        (try (cdq.context/skill-text ctx props)
                                                             (catch Throwable t
                                                               (default-property-tooltip-text ctx props))))}}
   :property.type/misc {:of-type? (fn [{:keys [creature/hp
                                               creature/species
                                               item/slot
                                               skill/effect]}]
                                    (not (or hp species slot effect)))
                        :sort-order 5
                        :title "Misc"
                        :overview {:title "Misc"
                                   :columns 10
                                   :image/dimensions [96 96]
                                   :tooltip-text-fn default-property-tooltip-text}}})

(defn property-type [props]
  (some (fn [[prop-type {:keys [of-type?]}]]
          (when (of-type? props)
            prop-type))
        property-types))

(comment
 ; weapons get branded as items
 ; but they are ALSO STILL items
 ; idk i am confused
 ; get all items should get them
 ; items with slot = weapon just have modifier: skill
 ; itself and a bit different schema
 (let [ctx @gdl.app/current-context
       properties (:context/properties ctx)
       properties (map (fn [[id props]]
                         (assoc props
                                :property/type
                                (case (property-type props)
                                  :species :property.type/species
                                  :creature :property.type/creature
                                  :weapon :property.type/weapon
                                  :item :property.type/item
                                  :skill :property.type/spell
                                  :misc :property.type/misc)))
                       properties)]
   (->> properties
        ;(take 10)
        sort-by-type
        (map serialize)
        (save-edn (:context/properties-file ctx))))
 )

(extend-type gdl.context.Context
  cdq.context/PropertyStore
  (get-property [{:keys [context/properties]} id]
    (safe-get properties id))

  (all-properties [{:keys [context/properties]} property-type]
    (filter (:of-type? (get property-types property-type)) (vals properties))))

(require 'gdl.backends.libgdx.context.image-drawer-creator)

(defn- deserialize-image [context {:keys [file sub-image-bounds]}]
  {:pre [file sub-image-bounds]}
  (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
        [tilew tileh]       (drop 2 sub-image-bounds)]
    ; TODO get-sprite does not return Image record => do @ image itself.
    (gdl.backends.libgdx.context.image-drawer-creator/map->Image
     (get-sprite context
                 {:file file
                  :tilew tileh
                  :tileh tilew}
                 [(int (/ sprite-x tilew))
                  (int (/ sprite-y tileh))]))))

(defn- serialize-image [image]
  (select-keys image [:file :sub-image-bounds]))

(defn- deserialize-animation [context {:keys [frames frame-duration looping?]}]
  (animation/create (map #(deserialize-image context %) frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defn- serialize-animation [animation]
  (-> animation
      (update :frames #(map serialize-image %))
      (select-keys [:frames :frame-duration :looping?])))

(defn- deserialize [context data]
  (->> data
       (#(if (:property/image %)
           (update % :property/image (fn [img] (deserialize-image context img)))
           %))
       (#(if (:animation %)
           (update % :animation (fn [anim] (deserialize-animation context anim)))
           %))))

; Other approaches to serialization:
; * multimethod & postwalk like cdq & use records ... or metadata hmmm , but then have these records there with nil fields etc.
; * print-dup prints weird stuff like #Float 0.5
; * print-method fucks up console printing, would have to add methods and remove methods during save/load
; => simplest way: just define keys which are assets (which are all the same anyway at the moment)
(defn- serialize [data]
  (->> data
       (#(if (:property/image %) (update % :property/image serialize-image) %))
       (#(if (:animation %) (update % :animation serialize-animation) %))))

(defn- load-edn [context file]
  (let [properties (-> file slurp edn/read-string)] ; TODO use .internal Gdx/files  => part of context protocol
    (assert (apply distinct? (map :property/id properties)))
    (->> properties
         (map #(deserialize context %))
         (#(zipmap (map :property/id %) %)))))

(defn ->context [context file]
  {:context/properties (load-edn context file)
   :context/properties-file file})

(defn- save-edn [file data]
  (binding [*print-level* nil]
    (->> data
         clojure.pprint/pprint
         with-out-str
         (spit file))))

(defn- sort-by-type [properties]
  (sort-by #(-> % property-type property-types :sort-order)
           properties))

(defn- write-to-file! [properties properties-file]
  (->> properties
       vals
       sort-by-type
       (map serialize)
       (save-edn properties-file)))

(defn update-and-write-to-file! [{:keys [context/properties
                                         context/properties-file] :as context}
                                 {:keys [property/id] :as data}]
  {:pre [(contains? data :property/id)
         (contains? properties id)
         (= (set (keys data))
            (set (keys (get properties id))))]}
  (println "update-and-write-to-file!")
  (binding [*print-level* nil] (clojure.pprint/pprint data)) ; TODO modal window with data / maybe get change diff ?
  (let [properties (update properties id merge data)]
    (.start (Thread. (fn []
                       (write-to-file! properties properties-file))))
    (assoc context :context/properties properties)))
