(ns cdq.context.properties
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [gdl.context :refer [get-sprite create-image]]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get readable-number]]
            [cdq.api.context :refer [modifier-text effect-text]]))

(defn property->type [property-types property]
  (some (fn [[type {:keys [of-type?]}]]
          (when (of-type? property)
            type))
        property-types))

;;

(defmulti property->text (fn [{:keys [context/properties]} property]
                           (property->type (:property-types properties) property)))

; TODO ?
(defmethod property->text :default [_ctx properties]
  #_(cons [:TODO (property-type properties)]
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
  (get-property [{{:keys [db]} :context/properties} id]
    (safe-get db id))

  (all-properties [{{:keys [db property-types]} :context/properties} property-type]
    (filter (:of-type? (get property-types property-type)) (vals db))))

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

(defn- validate [property-types property & {:keys [humanize?]}]
  (let [ptype (property->type property-types property)]
    (if-let [schema (:schema (get property-types ptype))]
      (if (try (m/validate schema property)
               (catch Throwable t
                 (throw (ex-info "m/validate fail" {:property property :ptype ptype} t))))
        property
        (throw (Error. (let [explained (m/explain schema property)]
                         (str (if humanize?
                                (me/humanize explained)
                                (binding [*print-level* nil]
                                  (with-out-str
                                   (clojure.pprint/pprint
                                    explained)))))))))
      property)))

(defn- load-edn [context property-types file]
  (let [properties (-> file slurp edn/read-string)] ; TODO use .internal Gdx/files  => part of context protocol
    (assert (apply distinct? (map :property/id properties)))
    (->> properties
         (map #(validate property-types %))
         (map #(deserialize context %))
         (#(zipmap (map :property/id %) %)))))

(defn ->context [context {:keys [file property-types]}]
  (let [properties {:file file
                    :property-types property-types}]
    (assoc properties :db (load-edn context property-types file))))

(defn- pprint-spit [file data]
  (binding [*print-level* nil]
    (->> data
         clojure.pprint/pprint
         with-out-str
         (spit file))))

(defn- sort-by-type [property-types properties-values]
  (sort-by #(->> %
                 (property->type property-types)
                 property-types
                 :edn-file-sort-order)
           properties-values))

(def ^:private write-to-file? true)

(defn- write-properties-to-file! [{:keys [db file property-types]}]
  (when write-to-file?
    (.start
     (Thread.
      (fn []
        (->> db
             vals
             (sort-by-type property-types)
             (map serialize)
             (map #(into (sorted-map) %))
             (pprint-spit file)))))))

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
     (swap! gdl.app/current-context update :context/properties update! prop))
   (def ^:private write-to-file? true)
   (swap! gdl.app/current-context update :context/properties update! (cdq.api.context/get-property ctx :creatures/vampire))
   nil)
 )

(defn update! [{:keys [db property-types] :as properties}
               {:keys [property/id] :as property}]
  {:pre [(contains? property :property/id) ; <=  part of validate - but misc does not have property/id -> add !
         (contains? db id)]}
  (validate property-types property :humanize? true)
  ;(binding [*print-level* nil] (clojure.pprint/pprint property))
  (let [properties (update properties :db assoc id property)]
    (write-properties-to-file! properties)
    properties))

(defn delete! [{:keys [db] :as properties}
               property-id]
  {:pre [(contains? db property-id)]}
  (let [properties (update properties :db dissoc property-id)]
    (write-properties-to-file! properties)
    properties))
