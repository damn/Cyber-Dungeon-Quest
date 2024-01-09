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
            cdq.effect.all
            [cdq.context :refer [modifier-text effect-text]]))

(def attributes {})

; TODO attr schema !

(defn- defattr [k data]
  (alter-var-root #'attributes assoc k data))

(defattr :property/image {:widget :image
                          :schema :some})

(defattr :property/animation {:widget :animation
                              :schema :some})

; TODO >+ max bodyt size?
(defattr :property/dimensions {:widget :label
                               :schema [:tuple pos? pos?]})

(defattr :creature/species {:widget :label
                            :schema [:qualified-keyword {:namespace :species}]})

(defattr :property/sound {:widget :sound
                          :schema :string})

(defattr :property/pretty-name {:widget :text-field
                                :schema :string})

(defattr :effect/sound {:widget :sound
                        :schema :string})

(defattr :damage/type {:widget :enum
                       :items [:physical :magic]})

(defattr :damage/min-max {:widget :text-field})

(defattr :effect/damage {:widget :nested-map
                         :components [:damage/type :damage/min-max]
                         :add-components? false
                         :schema [:map {:closed true}
                                  [:damage/type [:enum :physical :magic]]
                                  [:damage/min-max (m/form val-max-schema)]]})

(defattr :effect/spawn {:widget :text-field
                        :schema [:qualified-keyword {:namespace :creatures}]})

(defattr :effect/stun {:widget :text-field
                       :schema [:and number? pos?]})

(defattr :effect/restore-hp-mana {:widget :text-field
                                  :schema [:= true]})

(defattr :effect/projectile {:widget :text-field
                             :schema [:= true]})

(defattr :effect/target-entity {:widget :nested-map
                                :schema [:map {:closed true}
                                         [:hit-effect [:map]]
                                         [:maxrange pos?]]})

(def ^:private effect-attributes (keys (methods effect/transactions)))

(assert (= (set (filter #(= "effect" (namespace %)) (keys attributes)))
           (set effect-attributes)))

(def ^:private effect-components-schema
  (for [k effect-attributes]
    [k {:optional true} (:schema (get attributes k))]))

(defattr :hit-effect {:widget :nested-map
                      :components effect-attributes
                      :add-components? true})

(defattr :modifier/max-hp       {:widget :text-field :schema number?})
(defattr :modifier/max-mana     {:widget :text-field :schema number?})
(defattr :modifier/cast-speed   {:widget :text-field :schema number?})
(defattr :modifier/attack-speed {:widget :text-field :schema number?})

; TODO these 3 !
(defattr :modifier/shield       {:widget :text-field :schema :some})
(defattr :modifier/armor        {:widget :text-field :schema :some})
(defattr :modifier/damage       {:widget :text-field :schema :some})

(def ^:private modifier-attributes (keys modifier/modifier-definitions))

(assert (= (set (filter #(= "modifier" (namespace %)) (keys attributes)))
           (set modifier-attributes)))

(def ^:private modifier-components-schema
  (for [k modifier-attributes]
    [k {:optional true} (:schema (get attributes k))]))

(defattr :item/modifier {:widget :nested-map
                         :schema (vec (concat [:map {:closed true}] modifier-components-schema))
                         :components modifier-attributes
                         :add-components? true})

(defattr :item/slot {:widget :label
                     :schema [:qualified-keyword {:namespace :inventory.slot}]})

(defattr :skill/effect {:widget :nested-map
                        :schema (vec (concat [:map {:closed true}] effect-components-schema))
                        :components effect-attributes
                        :add-components? true})

(defn removable-attribute? [k]
  (#{"effect" "modifier"} (namespace k)))

(defattr :creature/faction {:widget :enum
                            :schema [:enum :good :evil]
                            :items [:good :evil]})

; TODO >0, <max-lvls (9 ?)
(defattr :creature/level {:widget :text-field
                          :schema [:maybe pos-int?]})

; TODO one of spells/skills
(defattr :creature/skills {:widget :one-to-many
                           :schema [:set :qualified-keyword]
                           :linked-property-type :property.type/spell})

; TODO one of items
(defattr :creature/items {:widget :one-to-many
                          :schema [:set :qualified-keyword]
                          :linked-property-type :property.type/item})

(defattr :creature/mana {:widget :text-field
                         :schema nat-int?})

(defattr :creature/flying? {:widget :check-box
                            :schema :boolean})

(defattr :creature/hp {:widget :text-field
                       :schema pos-int?})

(defattr :creature/speed {:widget :text-field
                          :schema pos?})

(defattr :creature/reaction-time {:widget :text-field
                                  :schema pos?})

(defattr :spell? {:widget :label
                  :schema [:= true]})

(defattr :skill/action-time {:widget :text-field
                             :schema pos?})

(defattr :skill/cooldown {:widget :text-field
                          :schema nat-int?})

(defattr :skill/cost {:widget :text-field
                      :schema nat-int?})

(defattr :maxrange {:widget :text-field})
(defattr :world/map-size {:widget :text-field})
(defattr :world/max-area-level {:widget :text-field})
(defattr :world/spawn-rate {:widget :text-field})

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
                                       :extra-info-text #(str (:creature/level %) (case (:creature/faction %)
                                                                                    :good "g"
                                                                                    :evil "e"))}
                            :schema (map-attribute-schema
                                     [:property/id [:qualified-keyword {:namespace :creatures}]]
                                     [:property/image
                                      :property/animation
                                      :property/dimensions
                                      :creature/species
                                      :creature/faction
                                      :creature/speed
                                      :creature/hp
                                      :creature/mana
                                      :creature/flying?
                                      :creature/reaction-time
                                      :creature/skills
                                      :creature/items
                                      :creature/level])}
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
                                   :spell?
                                   :skill/action-time
                                   :skill/cooldown
                                   :skill/cost
                                   :skill/effect])}
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
                                                           creature/flying?
                                                           creature/skills
                                                           creature/items
                                                           creature/level]}]
  [(str/capitalize (name id))
   (str/capitalize (name species))
   (when level (str "Level: " level))
   (str "Flying? " flying?)
   (when (seq skills) (str "Spells: " (str/join "," (map name skills))))
   (when (seq items) (str "Items: "   (str/join "," (map name items))))])

(def ^:private skill-cost-color "[CYAN]")
(def ^:private action-time-color "[GOLD]")
(def ^:private cooldown-color "[SKY]")
(def ^:private effect-color "[CHARTREUSE]")
(def ^:private modifier-color "[MODIFIER_BLUE]")

; TODO spell? why needed ... => use :property.type/spell or :property.type/weapon instead
; different enter active skill state sound
; different attack/cast speed modifier & text
; => dispatch on skill.type/weapon or skill.type/spell
; => :start-action-sound / :action-time-modifier / :action-time-pretty-name
(defmethod property->text :property.type/spell [ctx
                                                {:keys [property/id
                                                        skill/cost
                                                        skill/action-time
                                                        skill/cooldown
                                                        spell?
                                                        skill/effect]}]
  [(str/capitalize (name id))
   ;(if spell? "Spell" "Weapon")
   (when cost (str skill-cost-color "Cost: " cost "[]"))
   (str action-time-color (if spell?  "Cast-Time" "Attack-time") ": " (readable-number action-time) " seconds" "[]")
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
                                                         spell? ; TODO
                                                         skill/action-time
                                                         skill/effect]
                                                  :as item}]
  [(str pretty-name (when-let [cnt (:count item)] (str " (" cnt ")")))
   (when two-handed? "Two-handed")
   (str action-time-color (if spell?  "Cast-Time" "Attack-time") ": " (readable-number action-time) " seconds" "[]")
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
     (assoc ctx :effect/source-entity (:context/player-entity ctx))
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
       props (cdq.context/all-properties ctx :property.type/creature)
       props (for [prop props]
               (assoc prop :creature/reaction-time 0.2))]
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
  ;(binding [*print-level* nil] (clojure.pprint/pprint data))
  (let [properties (assoc properties id data)]
    (when write-to-file?
      (.start (Thread. (fn [] (write-to-file! properties properties-file)))))
    (assoc context :context/properties properties)))
