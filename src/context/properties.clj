(ns context.properties
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [gdl.context :refer [get-sprite]]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get readable-number]]
            context.modifier
            context.effect
            [cdq.context :refer [modifier-text effect-text]]))

(comment
 (do
  (require '[malli.provider :as mp])
  (set! *print-level* nil)
  (let [ctx @gdl.app/current-context]
    (->> :property.type/weapon
         (cdq.context/all-properties ctx)
         mp/provide
         clojure.pprint/pprint
         )))
 ; =>



 )

(set! com.kotcrab.vis.ui.widget.Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))

(comment
 (set! com.kotcrab.vis.ui.widget.Tooltip/DEFAULT_FADE_TIME (float 0.3))
 ;(set! com.kotcrab.vis.ui.widget.Tooltip/MOUSE_MOVED_FADEOUT false)
 ; always show BELOW the actor nicely not near mouse thing or ABOVE centered ?
 )

;com.kotcrab.vis.ui.widget.Tooltip
;static float 	DEFAULT_APPEAR_DELAY_TIME
;static float 	DEFAULT_FADE_TIME
;static boolean 	MOUSE_MOVED_FADEOUT
;Controls whether to fade out tooltip when mouse was moved.

; TODO
; * immediately show tooltips...

; * validation @ load/save of property-types attributes (optional ones to add like cooldown?)

; * text-field make validateabletextfield

; * schema/value-ranges/value-widgets for all modifiers/effects, e.g. damage select physical,magical,...

; * filter out not implemented weapons, etc.  mark them somehow

; aggro range wakup time, etc what else is hidden?!, unique death animation/sound/attacksound each weapon/spell etc.
; alert sound, etc., mana, hp, speed.... default block modifiers
; ASSERT & LOAD EDN / WRITE EDN / BEFORE SAVE DATA
; also things like target-entity props should be a map , :hit-effect & :maxrange, it was a list...

; Later
; * move other game params like aggro-range, wakeup/alert state time into creature props
; * unique death animations/sounds
; * attacksound each weapon/spell/effect/etc. grep play-sound! & image/animation all in properties.edn
; * sounds play/open bfxr
; * open tmx file, tiled editor
; * components editor creatures, etc. & builder
; * also item needs to be in certain slot, each slot only once, etc. also max-items ...?

(defn one-to-many-attribute->linked-property-type [k]
  (case k
    :creature/skills :property.type/spell
    :creature/items  :property.type/item))

; TODO label does not exist anymore.
; maybe no default widget & assert for all attributes are explicitly defined?
; also :property-widget/foo
(def attribute->value-widget
  {:property/id :label
   :property/image :image
   :property/pretty-name :text-field
   :item/slot :label
   :item/modifier :nested-map
   :modifier/armor :text-field
   :modifier/max-mana :text-field
   :weapon/two-handed? :label
   :creature/level :text-field
   :creature/species :link-button
   :creature/skills :one-to-many
   :creature/items :one-to-many
   :creature/hp :text-field
   :creature/speed :text-field
   :spell? :label
   :skill/action-time :text-field
   :skill/cooldown :text-field
   :skill/cost :text-field
   :skill/effect :nested-map
   :effect/sound :sound
   :effect/damage :text-field
   :effect/target-entity :nested-map
   :maxrange :text-field
   :hit-effect :nested-map
   :world/map-size :text-field
   :world/max-area-level :text-field
   :world/spawn-rate :text-field
   :world/princess :label})

(defn removable-attribute? [k]
  (#{"effect" "modifier"} (namespace k)))

(defn nested-map-attribute-add-components? [k]
  (#{:item/modifier :skill/effect :hit-effect} k))

(defn nested-map->components [k]
  (case k
    :item/modifier (keys context.modifier/modifier-definitions)
    :skill/effect (keys (methods context.effect/do!))
    :hit-effect   (keys (methods context.effect/do!)) ; TODO only those with 'source/target'
    ))

(defn attribute-widget-sort-attributes [properties]
  (sort-by
   (fn [[k _v]]
     [(case k
        :property/id 0
        :property/image 1
        :property/pretty-name 2
        :spell? 3
        :creature/level 3
        :item/slot 3
        :weapon/two-handed? 4
        :creature/species 4
        9)
      (name k)])
   properties))

; https://github.com/metosin/malli#built-in-schemas
(def property-types
  {:property.type/creature {:of-type? :creature/species
                            :edn-file-sort-order 1
                            :title "Creature"
                            :overview {:title "Creatures"
                                       :columns 16
                                       :image/dimensions [65 65]
                                       :sort-by-fn #(vector (or (:creature/level %) 9)
                                                            (name (:creature/species %))
                                                            (name (:property/id %)))
                                       :extra-info-text #(str (:creature/level %))}
                            :schema (m/schema
                                     [:map {:closed true}
                                      [:property/id [:qualified-keyword {:namespace :creatures}]]
                                      [:property/image :some]
                                      [:creature/species [:qualified-keyword {:namespace :species}]] ; one of species
                                      [:creature/skills [:set :qualified-keyword]] ; one of spells/skills
                                      [:creature/items  [:set :qualified-keyword]] ; one of items
                                      [:creature/level [:maybe pos-int?]]])} ; >0, <max-lvls (9 ?)
   :property.type/species {:of-type? :creature/hp
                           :edn-file-sort-order 2
                           :title "Species"
                           :overview {:title "Species"
                                      :columns 2}}
   :property.type/spell {:of-type? (fn [{:keys [item/slot skill/effect]}]
                                     (and (not slot) effect))
                         :edn-file-sort-order 0
                         :title "Spell"
                         :overview {:title "Spells"
                                    :columns 16
                                    :image/dimensions [70 70]}
                         :schema (m/schema
                                  [:map {:closed true}
                                   [:property/id [:qualified-keyword {:namespace :spells}]]
                                   [:property/image :some]
                                   [:spell? :boolean] ; true
                                   [:skill/action-time pos?]
                                   [:skill/cooldown nat-int?]
                                   [:skill/cost nat-int?]
                                   [:skill/effect [:map ]]])} ; of one of 'effect/' components
   ; weapons before items checking
   :property.type/weapon {:of-type? (fn [{:keys [item/slot]}]
                                      (and slot (= slot :inventory.slot/weapon)))
                          :edn-file-sort-order 4
                          :title "Weapon"
                          :overview {:title "Weapons"
                                     :columns 10
                                     :image/dimensions [96 96]}
                          :schema
                          (m/schema ; TODO DRY with spell ....
                           [:map
                            [:property/id [:qualified-keyword {:namespace :items}]]
                            [:property/pretty-name :string]
                            [:item/slot :qualified-keyword] ; :inventory.slot/weapon
                            [:weapon/two-handed? :boolean]
                            [:skill/action-time {:optional true} [:maybe pos?]] ; not optional
                            [:property/image :some]
                            [:skill/effect {:optional true} [:map]]])}
   :property.type/item {:of-type? :item/slot
                        :edn-file-sort-order 3
                        :title "Item"
                        :overview {:title "Items"
                                   :columns 17
                                   :image/dimensions [60 60]
                                   :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                          (name slot)
                                                          "")
                                                        (name (:property/id %)))}}
   :property.type/world {:of-type? :world/princess
                         :edn-file-sort-order 5
                         :title "World"
                         :overview {:title "Worlds"
                                    :columns 10
                                    :image/dimensions [96 96]}}
   :property.type/misc {:of-type? (fn [{:keys [creature/hp
                                               creature/species
                                               item/slot
                                               skill/effect
                                               world/princess]}]
                                    (not (or hp species slot effect princess)))
                        :edn-file-sort-order 6
                        :title "Misc"
                        :overview {:title "Misc"
                                   :columns 10
                                   :image/dimensions [96 96]}}
   })

(defn property-type [property]
  (some (fn [[type {:keys [of-type?]}]]
          (when (of-type? property)
            type))
        property-types))

;;

(defmulti property->text (fn [_ctx property] (property-type property)))

(defmethod property->text :default [_ctx properties]
  (cons [:TODO (property-type properties)]
        properties))

(defmethod property->text :property.type/creature [_ctx
                                                   {:keys [property/id
                                                           creature/species
                                                           creature/skills
                                                           creature/items
                                                           creature/level]}]
  [(str/capitalize (name id))
   (str "Species: " (str/capitalize (name species)))
   (when level (str "Level: " level))
   (when (seq skills) (str "Spells: " (str/join "," (map name skills))))
   (when (seq items) (str "Items: "   (str/join "," (map name items))))])

; TODO spell? why needed ... => use :property.type/spell or :property.type/weapon instead
; different enter active skill state sound
; different attack/cast speed modifier & text
(defmethod property->text :property.type/spell [{:keys [context/player-entity] :as context}
                                                {:keys [property/id
                                                        skill/cost
                                                        skill/action-time
                                                        skill/cooldown
                                                        spell?
                                                        skill/effect]}]
  [(str/capitalize (name id))
   (if spell? "Spell" "Weapon")
   (when cost (str "Cost: " cost))
   (str (if spell?  "Cast-Time" "Attack-time") ": " (readable-number action-time) " seconds")
   (when cooldown (str "Cooldown: " (readable-number cooldown)))
   (effect-text (merge context {:effect/source player-entity})
                effect)])

(defmethod property->text :property.type/item [ctx
                                               {:keys [property/pretty-name
                                                       item/modifier]
                                                :as item}]
  [(str pretty-name (when-let [cnt (:count item)] (str " (" cnt ")")))
   (when modifier (modifier-text ctx modifier))])

(defmethod property->text :property.type/weapon [{:keys [context/player-entity] :as ctx}
                                                 {:keys [property/pretty-name
                                                         item/two-handed?
                                                         item/modifier
                                                         spell? ; TODO
                                                         skill/action-time
                                                         skill/effect]
                                                  :as item}]
  [(str pretty-name (when-let [cnt (:count item)] (str " (" cnt ")")))
   (when two-handed? "Two-handed")
   (str (if spell?  "Cast-Time" "Attack-time") ": " (readable-number action-time) " seconds") ; TODO
   (when modifier (modifier-text ctx modifier))
   (effect-text (merge ctx {:effect/source player-entity})
                effect)])

(extend-type gdl.context.Context
  cdq.context/TooltipText
  (tooltip-text [ctx property]
    (try (->> property
              (property->text ctx)
              (remove nil?)
              (str/join "\n"))
         (catch Throwable t
           (str t))))) ; TODO not implemented weapons.

;;

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

(defn- validate [property & {:keys [humanize?]}]
  (if-let [schema (:schema (get property-types (property-type property)))]
    (if (m/validate schema property)
      property
      (throw (Error. (let [explained (m/explain schema property)]
                       (str (if humanize?
                              (me/humanize explained)
                              (binding [*print-level* nil]
                                (with-out-str
                                 (clojure.pprint/pprint
                                  explained)))))))))
    property))

(defn- load-edn [context file]
  (let [properties (-> file slurp edn/read-string)] ; TODO use .internal Gdx/files  => part of context protocol
    (assert (apply distinct? (map :property/id properties)))
    (->> properties
         (map validate)
         (map #(deserialize context %))
         (#(zipmap (map :property/id %) %)))))

(defn ->context [context file]
  {:context/properties (load-edn context file)
   :context/properties-file file})

(defn- pprint-spit [file data]
  (binding [*print-level* nil]
    (->> data
         clojure.pprint/pprint
         with-out-str
         (spit file))))

(defn- sort-by-type [properties]
  (sort-by #(-> % property-type property-types :edn-file-sort-order)
           properties))

(defn- write-to-file! [properties properties-file]
  (->> properties
       vals
       sort-by-type
       (map serialize)
       (pprint-spit properties-file)))


(defn update-and-write-to-file! [{:keys [context/properties
                                         context/properties-file] :as context}
                                 {:keys [property/id] :as data}]
  {:pre [(contains? data :property/id)
         (contains? properties id)
         (= (set (keys data))
            (set (keys (get properties id))))]}
  (validate data :humanize? true)
  (println "\nupdate-and-write-to-file!")
  (binding [*print-level* nil]
    (clojure.pprint/pprint data)) ; TODO modal window with data / maybe get change diff ?
  (let [properties (update properties id merge data)]
    (.start (Thread. (fn []
                       (write-to-file! properties properties-file))))
    (assoc context :context/properties properties)))
