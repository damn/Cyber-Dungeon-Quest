(ns cdq.context.properties
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [gdl.context :refer [get-sprite create-image]]
            [gdl.graphics.animation :as animation]
            [data.val-max :refer [val-max-schema]]
            [utils.core :refer [safe-get readable-number]]
            [cdq.context.modifier :as modifier]
            cdq.context.modifier.all
            [cdq.effect :as effect]
            cdq.tx.all
            [cdq.context :refer [modifier-text effect-text]]))

(def attributes {})

; TODO attr schema !
; * for adding entity components -> need default-value ???
; TODO all used @ :components (tx/,modifier) = add default-value
; (nested map attr needs default-value to add, but I can place this in the components right?)
; see damage
; min-max no schema?
; TODO modifer shield/damage/armor not done, validatable
; TODO one-tomany - one of spells/skills
; TODO one-tomany - one of items
; TODO body assert >+ min body size?
; TODO entity non removable! are removalbe right now!, what is optional, what depends on what?
; reaction time?
; TODO also which attribute is optional or not ? see ... default-values calculate from that?
; TODO creature lvl >0, <max-lvls (9 ?)

; TODO ___ weapon / skill / item mess ____

; TODO make misc is when no property-type matches ? :else case?

(comment
 ; this is not done anymore @ tx/entity -> do it? how to?
 (def ^:private modifier-attributes (keys modifier/modifier-definitions))
 (assert (= (set (filter #(= "modifier" (namespace %)) (keys attributes)))
            (set modifier-attributes))))

(defn- defattribute [k data]
  (alter-var-root #'attributes assoc k data))

(def ^:private sound    {:widget :sound     :schema :string})
(def ^:priate  image    {:widget :image     :schema :some})
(def ^:private animation {:widget :animation :schema :some})

(defn- enum [& items]
  {:widget :enum
   :schema (apply vector :enum items)
   :items items})

(defattribute :property/image image)

(defattribute :entity/animation animation)

(defattribute :entity/body {:widget :nested-map
                            :schema [:map {:closed true}
                                     [:width pos?]
                                     [:height pos?]
                                     [:solid? :boolean]]
                            :default-value {:width 0.5
                                            :height 0.5
                                            :solid? true}})

(defattribute :entity/faction (enum :good :evil))

(defattribute :entity/skills {:widget :one-to-many
                              :schema [:set :qualified-keyword]
                              :linked-property-type :property.type/spell})

(defattribute :entity/inventory {:widget :one-to-many
                                 :schema [:set :qualified-keyword]
                                 :linked-property-type :property.type/item})

(defattribute :entity/mana {:widget :text-field
                            :schema nat-int?})

(defattribute :entity/flying? {:widget :check-box
                               :schema :boolean
                               :default-value true})

(defattribute :entity/hp {:widget :text-field
                          :schema pos-int?})

(defattribute :entity/movement {:widget :text-field
                                :schema pos?})

(defattribute :entity/reaction-time {:widget :text-field
                                     :schema pos?})

(defattribute :property/sound sound)

(defattribute :tx/sound sound)

(defattribute :property/pretty-name {:widget :text-field
                                     :schema :string})

(defattribute :damage/type (enum :physical :magic))

(defattribute :damage/min-max {:widget :text-field})

(defattribute :tx/damage {:widget :nested-map
                          :schema [:map {:closed true}
                                   [:damage/type [:enum :physical :magic]]
                                   [:damage/min-max (m/form val-max-schema)]]
                          :default-value {:damage/type :physical
                                          :damage/min-max [1 10]}})

(defattribute :tx/spawn {:widget :text-field
                         :schema [:qualified-keyword {:namespace :creatures}]})

(defattribute :tx/stun {:widget :text-field
                        :schema [:and number? pos?]})

(defattribute :tx/restore-hp-mana {:widget :text-field
                                   :schema [:= true]})

(defattribute :tx/projectile {:widget :text-field
                              :schema [:= true]})

(defattribute :maxrange {:widget :text-field})

(defattribute :tx/target-entity {:widget :nested-map
                                 :schema [:map {:closed true}
                                          [:hit-effect [:map]]
                                          [:maxrange pos?]]
                                 :default-value {:hit-effect {}
                                                 :max-range 2.0}})

(defattribute :modifier/max-hp       {:widget :text-field :schema number?})
(defattribute :modifier/max-mana     {:widget :text-field :schema number?})

(defattribute :modifier/cast-speed   {:widget :text-field :schema pos?})
(defattribute :modifier/attack-speed {:widget :text-field :schema pos?})

(defattribute :modifier/shield {:widget :text-field :schema :some})
(defattribute :modifier/armor  {:widget :text-field :schema :some})
(defattribute :modifier/damage {:widget :text-field :schema :some})

(defn removable-attribute? [k]
  (#{"tx" "modifier" "entity"} (namespace k)))

(defn- components-attribute [component-namespace]
  (let [component-attributes (filter #(= (name component-namespace) (namespace %))
                                     (keys attributes))]
    {:widget :nested-map
     :schema (vec (concat [:map {:closed true}]
                          (for [k component-attributes]
                            [k {:optional true} (:schema (get attributes k))])))
     :components component-attributes}))

(defattribute :property/entity (components-attribute :entity))
(defattribute :skill/effect    (components-attribute :tx))
(defattribute :hit-effect      (components-attribute :tx))
(defattribute :item/modifier   (components-attribute :modifier))

(defattribute :item/slot {:widget :label
                          :schema [:qualified-keyword {:namespace :inventory.slot}]})


(defattribute :creature/species {:widget :label
                                 :schema [:qualified-keyword {:namespace :species}]})

(defattribute :creature/level {:widget :text-field
                               :schema [:maybe pos-int?]})

(defattribute :skill/start-action-sound sound)

(defattribute :skill/action-time-modifier-key (enum :stats/cast-speed :stats/attack-speed))

(defattribute :skill/action-time {:widget :text-field
                                  :schema pos?})

(defattribute :skill/cooldown {:widget :text-field
                               :schema nat-int?})

(defattribute :skill/cost {:widget :text-field
                           :schema nat-int?})

(defattribute :world/map-size {:widget :text-field})
(defattribute :world/max-area-level {:widget :text-field})
(defattribute :world/spawn-rate {:widget :text-field})

(defn- map-attribute-schema [id-attribute attr-ks]
  (m/schema
   (vec (concat [:map {:closed true} id-attribute]
                (for [k attr-ks]
                  (vector k (:schema (get attributes k))))))))

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
                                       :extra-info-text #(str (:creature/level %)
                                                              (case (:entity/faction (:property/entity %))
                                                                :good "g"
                                                                :evil "e"))}
                            :schema (map-attribute-schema
                                     [:property/id [:qualified-keyword {:namespace :creatures}]]
                                     [:property/image
                                      :creature/species
                                      :creature/level
                                      :property/entity])}

   :property.type/spell {:of-type? (fn [{:keys [item/slot skill/effect]}]
                                     (and (not slot) effect))
                         :edn-file-sort-order 0
                         :title "Spell"
                         :overview {:title "Spells"
                                    :columns 16
                                    :image/dimensions [70 70]}
                         :schema (map-attribute-schema
                                  [:property/id [:qualified-keyword {:namespace :spells}]]
                                  [:property/image
                                   :skill/action-time
                                   :skill/cooldown
                                   :skill/cost
                                   :skill/effect
                                   :skill/start-action-sound
                                   :skill/action-time-modifier-key])}

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
                                    [:skill/effect {:optional true} [:map ]] ; can be nil not implemented weapons.
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
                        :schema (map-attribute-schema
                                 [:property/id [:qualified-keyword {:namespace :items}]]
                                 [:property/pretty-name
                                  :item/slot
                                  :property/image
                                  :item/modifier])}

   :property.type/world {:of-type? :world/princess
                         :edn-file-sort-order 5
                         :title "World"
                         :overview {:title "Worlds"
                                    :columns 10
                                    :image/dimensions [96 96]}}

   :property.type/misc {:of-type? (fn [{:keys [entity/hp
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

(comment
 (defn- all-text-colors []
   (let [colors (seq (.keys (com.badlogic.gdx.graphics.Colors/getColors)))]
     (str/join "\n"
               (for [colors (partition-all 4 colors)]
                 (str/join " , " (map #(str "[" % "]" %) colors)))))))

(com.badlogic.gdx.graphics.Colors/put "ITEM_GOLD"
                                      (com.badlogic.gdx.graphics.Color. (float 0.84)
                                                                        (float 0.8)
                                                                        (float 0.52)
                                                                        (float 1)))

(com.badlogic.gdx.graphics.Colors/put "MODIFIER_BLUE"
                                      (com.badlogic.gdx.graphics.Color. (float 0.38)
                                                                        (float 0.47)
                                                                        (float 1)
                                                                        (float 1)))

(defmethod property->text :property.type/creature [_ctx
                                                   {:keys [property/id
                                                           creature/species
                                                           entity/flying?
                                                           entity/skills
                                                           entity/inventory
                                                           creature/level]}]
  [(str/capitalize (name id))
   (str/capitalize (name species))
   (when level (str "Level: " level))
   (str "Flying? " flying?)
   (when (seq skills) (str "Spells: " (str/join "," (map name skills))))
   (when (seq inventory) (str "Items: "   (str/join "," (map name inventory))))])

(def ^:private skill-cost-color "[CYAN]")
(def ^:private action-time-color "[GOLD]")
(def ^:private cooldown-color "[SKY]")
(def ^:private effect-color "[CHARTREUSE]")
(def ^:private modifier-color "[VIOLET]")

(defmethod property->text :property.type/spell [ctx
                                                {:keys [property/id
                                                        skill/cost
                                                        skill/action-time
                                                        skill/cooldown
                                                        skill/effect]}]
  [(str/capitalize (name id))
   (when cost (str skill-cost-color "Cost: " cost "[]"))
   (str action-time-color "Cast-Time: " (readable-number action-time) " seconds" "[]")
   (when cooldown (str cooldown-color "Cooldown: " (readable-number cooldown) "[]"))
   (str effect-color (effect-text ctx effect) "[]")])

(defmethod property->text :property.type/item [ctx
                                               {:keys [property/pretty-name
                                                       item/modifier]
                                                :as item}]
  [(str "[ITEM_GOLD]" pretty-name (when-let [cnt (:count item)] (str " (" cnt ")")) "[]")
   (when (seq modifier) (str modifier-color (modifier-text ctx modifier) "[]"))])

(defmethod property->text :property.type/weapon [ctx
                                                 {:keys [property/pretty-name
                                                         item/two-handed?
                                                         item/modifier
                                                         skill/action-time
                                                         skill/effect]
                                                  :as item}]
  [(str pretty-name (when-let [cnt (:count item)] (str " (" cnt ")")))
   (when two-handed? "Two-handed")
   (str action-time-color "Attack-time: " (readable-number action-time) " seconds" "[]")
   (when (seq modifier) (str modifier-color (modifier-text ctx modifier) "[]"))
   (str effect-color (effect-text ctx effect) "[]")])

(extend-type gdl.context.Context
  cdq.context/TooltipText
  (tooltip-text [ctx property]
    (try (->> property
              (property->text ctx)
              (remove nil?)
              (str/join "\n"))
         (catch Throwable t
           (str t)))); TODO not implemented weapons.

  (player-tooltip-text [ctx property]
    (cdq.context/tooltip-text
     (assoc ctx :effect/source (:context/player-entity ctx))
     property)))

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
       ; audiovisual
       (#(if (:entity/animation %)
           (update % :entity/animation (fn [anim] (deserialize-animation context anim)))
           %))
       (#(if (:entity/animation (:property/entity %))
           (update-in % [:property/entity :entity/animation] (fn [anim] (deserialize-animation context anim)))
           %))))

; Other approaches to serialization:
; * multimethod & postwalk like cdq & use records ... or metadata hmmm , but then have these records there with nil fields etc.
; * print-dup prints weird stuff like #Float 0.5
; * print-method fucks up console printing, would have to add methods and remove methods during save/load
; => simplest way: just define keys which are assets (which are all the same anyway at the moment)
(defn- serialize [data]
  (->> data
       (#(if (:property/image %) (update % :property/image serialize-image) %))
       ; audiovisual
       (#(if (:entity/animation %)
           (update % :entity/animation serialize-animation) %))
       (#(if (:entity/animation (:property/entity %))
           (update-in % [:property/entity :entity/animation] serialize-animation) %))))

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

(def ^:private write-to-file? true)

(defn- write-properties-to-file! [{:keys [context/properties
                               context/properties-file]}]
  (when write-to-file?
    (.start
     (Thread.
      (fn []
        (->> properties
             vals
             sort-by-type
             (map serialize)
             (map #(into (sorted-map) %))
             (pprint-spit properties-file)))))))


(comment
 ; # Add new attributes
 (let [ctx @gdl.app/current-context
       props (cdq.context/all-properties ctx :property.type/weapon)
       props (for [prop props]
               (-> prop
                   (assoc :skill/start-action-sound "sounds/slash.wav"
                          :skill/action-time-modifier-key :attack-speed)))]
   (def write-to-file? false)
   (doseq [prop props]
     (swap! gdl.app/current-context update! prop))
   (def ^:private write-to-file? true)
   (swap! gdl.app/current-context update! (cdq.context/get-property ctx :creatures/vampire))
   nil)
 )

(defn update! [{:keys [context/properties] :as context}
               {:keys [property/id] :as data}]
  {:pre [(contains? data :property/id)
         (contains? properties id)]}
  (validate data :humanize? true)
  ;(binding [*print-level* nil] (clojure.pprint/pprint data))
  (let [context (update context :context/properties assoc id data)]
    (write-properties-to-file! context)
    context))

(defn delete! [{:keys [context/properties
                       context/properties-file] :as context}
               property-id]
  {:pre [(contains? properties property-id)]}
  (let [context (update context :context/properties dissoc property-id)]
    (write-properties-to-file! context)
    context))
