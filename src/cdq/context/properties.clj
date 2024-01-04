(ns cdq.context.properties
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [gdl.context :refer [get-sprite create-image]]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get readable-number]]
            cdq.context.modifier
            cdq.context.modifier.all
            cdq.context.effect
            cdq.context.effect.all
            [cdq.context :refer [modifier-text effect-text]]))

; TODO cannot find bat -> assertions @ properties load
; how can we validate effect/spawn value is one of creatures
; if we load our properties all at once and validate step by step ?
; should we use a database ?? idk. maybe...
; https://docs.datomic.com/pro/schema/schema.html?search=%20
; :skill/effect {:effect/spawn :bat},
; should have been :creatures/bat

(comment
 (do
  (require '[malli.provider :as mp])
  (set! *print-level* nil)
  (let [ctx @gdl.app/current-context]
    (->> :property.type/item
         (cdq.context/all-properties ctx)
         mp/provide
         clojure.pprint/pprint
         )))
 )

(set! com.kotcrab.vis.ui.widget.Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))

(comment
 (set! com.kotcrab.vis.ui.widget.Tooltip/DEFAULT_FADE_TIME (float 0.3))
 ;(set! com.kotcrab.vis.ui.widget.Tooltip/MOUSE_MOVED_FADEOUT false)
 ; _IMPORTANT_
 ; TODO always show BELOW the/ABOVE actor nicely not near mouse thing or ABOVE centered ?
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
   :property/animation :animation
   :property/pretty-name :text-field
   :item/slot :label
   :item/modifier :nested-map
   :weapon/two-handed? :label
   :creature/faction :enum
   :creature/level :text-field
   :creature/skills :one-to-many
   :creature/items :one-to-many
   :creature/mana :text-field
   :creature/flying? :check-box
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

; TODO make each attribute a map with :widget and extra keys => move into 1 place...
(defn enum-attribute->items [k]
  (case k
    :creature/faction [:faction/good :faction/evil]))

(doseq [k (concat (keys cdq.context.modifier/modifier-definitions)
                  (keys (methods cdq.context.effect/do!)))]
  (alter-var-root #'attribute->value-widget (fn [m]
                                              (if (contains? m k) m (assoc m k :text-field)))))
; TODO schema/default-values/nicer text?

(defn removable-attribute? [k]
  (#{"effect" "modifier"} (namespace k)))

(defn nested-map-attribute-add-components? [k]
  (#{:item/modifier :skill/effect :hit-effect} k))

(defn nested-map->components [k]
  (case k
    :item/modifier (keys cdq.context.modifier/modifier-definitions)
    :skill/effect (keys (methods cdq.context.effect/do!))
    :hit-effect   (keys (methods cdq.context.effect/do!)) ; TODO only those with 'source/target'
    ))

(defn attribute-widget-sort-attributes [properties]
  (sort-by
   (fn [[k _v]]
     [(case k
        :property/id 0
        :property/image 1
        :property/animation 2
        :property/dimensions 3
        :property/pretty-name 2
        :spell? 3
        :creature/level 3
        :item/slot 3
        :weapon/two-handed? 4
        :creature/species 4
        :creature/faction 5
        :creature/flying? 6
        :creature/speed 7
        :creature/hp 8
        :creature/mana 9
        10)
      (name k)])
   properties))


(def ^:private effect-components-schema
  (for [k (keys (methods cdq.context.effect/do!))]
    [k {:optional true} (m/form (cdq.context.effect/value-schema k))]))

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
                                       :extra-info-text #(str (:creature/level %) (case (:creature/faction %)
                                                                                    :faction/good "g"
                                                                                    :faction/evil "e"))}
                            :schema (m/schema
                                     [:map {:closed true}
                                      [:property/id [:qualified-keyword {:namespace :creatures}]]
                                      [:property/image :some]
                                      [:property/dimensions [:tuple pos? pos?]] ; & > max size?
                                      [:property/animation :some]
                                      [:creature/species [:qualified-keyword {:namespace :species}]] ; one of species
                                      [:creature/faction [:enum :faction/good :faction/evil]]
                                      [:creature/speed pos?]
                                      [:creature/hp pos-int?]
                                      [:creature/mana nat-int?]
                                      [:creature/flying? :boolean]
                                      [:creature/skills [:set :qualified-keyword]] ; one of spells/skills
                                      [:creature/items  [:set :qualified-keyword]] ; one of items
                                      [:creature/level [:maybe pos-int?]]])} ; >0, <max-lvls (9 ?)
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
                                   [:skill/effect (vec (concat [:map {:closed true}]
                                                               effect-components-schema))]])}
   ; weapons before items checking
   :property.type/weapon {:of-type? (fn [{:keys [item/slot]}]
                                      (and slot (= slot :inventory.slot/weapon)))
                          :edn-file-sort-order 4
                          :title "Weapon"
                          :overview {:title "Weapons"
                                     :columns 10
                                     :image/dimensions [96 96]}
                          :schema (m/schema ; TODO DRY with spell/item
                                   [:map
                                    [:property/id [:qualified-keyword {:namespace :items}]]
                                    [:property/pretty-name :string]
                                    [:item/slot [:qualified-keyword {:namespace :inventory.slot}]] ; :inventory.slot/weapon
                                    [:property/image :some]
                                    [:weapon/two-handed? :boolean]
                                    [:skill/action-time {:optional true} [:maybe pos?]] ; not optional
                                    [:skill/effect {:optional true} [:map]]
                                    [:item/modifier [:map ]]])}
   :property.type/item {:of-type? :item/slot
                        :edn-file-sort-order 3
                        :title "Item"
                        :overview {:title "Items"
                                   :columns 17
                                   :image/dimensions [60 60]
                                   :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                          (name slot)
                                                          "")
                                                        (name (:property/id %)))}
                        :schema (m/schema
                                 [:map
                                  [:property/id [:qualified-keyword {:namespace :items}]]
                                  [:property/pretty-name :string]
                                  [:item/slot [:qualified-keyword {:namespace :inventory.slot}]]
                                  [:property/image :some]
                                  [:item/modifier [:map ]]])}
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
                                                           creature/flying?
                                                           creature/skills
                                                           creature/items
                                                           creature/level]}]
  [(str/capitalize (name id))
   (str "Species: " (str/capitalize (name species)))
   (when level (str "Level: " level))
   (str "Flying? " flying?)
   (when (seq skills) (str "Spells: " (str/join "," (map name skills))))
   (when (seq items) (str "Items: "   (str/join "," (map name items))))])

; TODO spell? why needed ... => use :property.type/spell or :property.type/weapon instead
; different enter active skill state sound
; different attack/cast speed modifier & text
; => dispatch on skill.type/weapon or skill.type/spell
; => :start-action-sound / :action-time-modifier / :action-time-pretty-name
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
   (when (seq modifier) (modifier-text ctx modifier))])

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
   (when (seq modifier) (modifier-text ctx modifier))
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
  {:pre [file]}
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      ; TODO get-sprite does not return Image record => do @ image itself.
      (gdl.backends.libgdx.context.image-drawer-creator/map->Image
       (get-sprite context
                   {:file file
                    :tilew tileh
                    :tileh tilew}
                   [(int (/ sprite-x tilew))
                    (int (/ sprite-y tileh))])))
    (create-image context file)))

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
       (#(if (:property/animation %)
           (update % :property/animation (fn [anim] (deserialize-animation context anim)))
           %))))

; Other approaches to serialization:
; * multimethod & postwalk like cdq & use records ... or metadata hmmm , but then have these records there with nil fields etc.
; * print-dup prints weird stuff like #Float 0.5
; * print-method fucks up console printing, would have to add methods and remove methods during save/load
; => simplest way: just define keys which are assets (which are all the same anyway at the moment)
(defn- serialize [data]
  (->> data
       (#(if (:property/image %) (update % :property/image serialize-image) %))
       (#(if (:property/animation %) (update % :property/animation serialize-animation) %))))

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

(def ^:private write-to-file? true)

(comment
 ; # Add new attributes
 (let [ctx @gdl.app/current-context
       props (cdq.context/all-properties ctx :property.type/item)
       props (for [prop props
                   :when (not (:item/modifier prop))]
               (assoc prop :item/modifier {}))]
   (def write-to-file? false)
   (doseq [prop props]
     (swap! gdl.app/current-context update-and-write-to-file! prop))
   (def ^:private write-to-file? true)
   (swap! gdl.app/current-context update-and-write-to-file! (cdq.context/get-property ctx :creatures/vampire))
   nil)
 )

(defn update-and-write-to-file! [{:keys [context/properties
                                         context/properties-file] :as context}
                                 {:keys [property/id] :as data}]
  {:pre [(contains? data :property/id)
         (contains? properties id)]}
  (validate data :humanize? true)
  (binding [*print-level* nil] (clojure.pprint/pprint data))
  (let [properties (assoc properties id data)]
    (when write-to-file?
      (.start (Thread. (fn [] (write-to-file! properties properties-file)))))
    (assoc context :context/properties properties)))
