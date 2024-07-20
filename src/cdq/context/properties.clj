(ns cdq.context.properties
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [gdl.context :refer [get-sprite create-image]]
            [gdl.graphics.animation :as animation]
            [data.val-max :refer [val-max-schema]]
            [utils.core :refer [safe-get readable-number]]
            [cdq.api.attributes :as attributes]
            [cdq.api.context :refer [modifier-text effect-text]]))

; TODO all this cdq.property-types // like attributes

; TODO make misc is when no property-type matches ? :else case?

; TODO similar to map-attribute & components-attribute
(defn- map-attribute-schema [id-attribute attr-ks]
  (m/schema
   (vec (concat [:map {:closed true} id-attribute] ; TODO same id-attribute w. different namespaces ...
                ; creature/id ?
                ; item/id ?
                (for [k attr-ks]
                  (vector k (:schema (get attributes/attributes k))))))))

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

   :property.type/skill {:of-type? :skill/effect
                         :edn-file-sort-order 0
                         :title "Skill"
                         :overview {:title "Skill"
                                    :columns 16
                                    :image/dimensions [70 70]}
                         :schema (map-attribute-schema
                                  [:property/id [:qualified-keyword {:namespace :skills}]]
                                  [:property/image
                                   :skill/action-time
                                   :skill/cooldown
                                   :skill/cost
                                   :skill/effect
                                   :skill/start-action-sound
                                   :skill/action-time-modifier-key])}

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
                                  :property/image
                                  :item/slot
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

; TODO cdq.tooltips?

(defmethod property->text :property.type/skill [ctx
                                                {:keys [property/id
                                                        skill/action-time
                                                        skill/cooldown
                                                        skill/cost
                                                        skill/effect
                                                        skill/action-time-modifier-key]}]
  [(str/capitalize (name id))
   (str skill-cost-color "Cost: " cost "[]")
   (str action-time-color
        (case action-time-modifier-key
          :stats/cast-speed "Casting-Time"
          :stats/attack-speed "Attack-Time")
        ": "
        (readable-number action-time) " seconds" "[]")
   (str cooldown-color "Cooldown: " (readable-number cooldown) "[]")
   (str effect-color (effect-text ctx effect) "[]")])

(defmethod property->text :property.type/item [ctx
                                               {:keys [property/pretty-name
                                                       item/modifier]
                                                :as item}]
  [(str "[ITEM_GOLD]" pretty-name (when-let [cnt (:count item)] (str " (" cnt ")")) "[]")
   (when (seq modifier) (str modifier-color (modifier-text ctx modifier) "[]"))])

(extend-type gdl.context.Context
  cdq.api.context/TooltipText
  (tooltip-text [ctx property]
    (try (->> property
              (property->text ctx)
              (remove nil?)
              (str/join "\n"))
         (catch Throwable t
           (str t)))); TODO not implemented weapons. ( ?! )

  (player-tooltip-text [ctx property]
    (cdq.api.context/tooltip-text
     (assoc ctx :effect/source (:context/player-entity ctx))
     property)))

;;

(extend-type gdl.context.Context
  cdq.api.context/PropertyStore
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
 ; # Add new attributes => make into fn for property-type apply fn to all props
 (let [ctx @gdl.app/current-context
       props (cdq.api.context/all-properties ctx :property.type/weapon)
       props (for [prop props]
               (-> prop
                   (assoc :skill/start-action-sound "sounds/slash.wav"
                          :skill/action-time-modifier-key :attack-speed)))]
   (def write-to-file? false)
   (doseq [prop props]
     (swap! gdl.app/current-context update! prop))
   (def ^:private write-to-file? true)
   (swap! gdl.app/current-context update! (cdq.api.context/get-property ctx :creatures/vampire))
   nil)
 )

(defn update! [{:keys [context/properties] :as context}
               {:keys [property/id] :as data}]
  {:pre [(contains? data :property/id) ; <=  part of validate - but misc does not have property/id -> add !
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
