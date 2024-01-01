(ns context.properties
  (:require [clojure.edn :as edn]
            [gdl.context :refer [get-sprite]]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get]]
            cdq.context))

; 1. step: proper namespaced keywords
; :property/id
; => where used everywhere ? OMG

; TODO also for entities: :entity/id  , :entity/position, etc.
; will make work easier in the future .. ? but no defrecord then hmm...

; maybe :spell? true not needed but property.type/spell and property.type/weapon

(comment

 (require '[malli.provider :as mp])

 (set! *print-level* nil)
 (let [ctx @gdl.app/current-context
       properties (:context/properties ctx)
       creatures (cdq.context/all-properties ctx :creature)
       ]


   (mp/provide creatures))
 [:map
  [:image
   [:map
    [:file :string]
    [:texture :some]
    [:sub-image-bounds [:vector :int]]
    [:scale :int]
    [:pixel-dimensions [:vector :int]]
    [:world-unit-dimensions [:vector integer?]]
    [:tilew :int]
    [:tileh :int]]]
  [:id :keyword]
  [:species :qualified-keyword]
  [:skills [:set :qualified-keyword]]
  [:items [:set :keyword]]
  [:level {:optional true} :int]]
 )

; TODO SKILL WINDOW OUTDATED PROPERTIES !! GETS ADDED TO ACTIOnBAR !! RELOAD ON SESSION START !
; also skill tooltip in property editor out of date

; Idea;
; during running game each entity has property/id
; can right click and edit the properties on the fly of _everything_
; in non-debug mode only presenting, otherwise editable.

; Validation at: read, write
; Each property type => keys for editing
; Each key: what is it -> widget, validation
; Weapon both item and skill possible? normalized & denormalized data
; (creature animations, weapons, spritesheet indices)
; sounds play/open bfxr
; open tmx file, tiled editor
; components editor creatures, etc. & builder
; * also item needs to be in certain slot, each slot only once, etc. also max-items ...?
; TODO aggro range wakup time, etc what else is hidden?!, unique death animation/sound/attacksound each weapon/spell etc.
; alert sound, etc., mana, hp, speed.... default block modifiers

(def ^:private prop-type-unique-key
  {:species :hp
   :creature :creature/species
   :item :slot
   :skill (fn [{:keys [slot skill/effect]}] (and (not slot) effect))
   :weapon (fn [{:keys [slot]}] (and slot (= slot :weapon)))
   :misc (fn [{:keys [hp creature/species slot skill/effect]}]
           (not (or hp species slot effect)))})

; TODO schema -
; => :property/type
; :property.type/creature
; :property.type/item
; :property.type/weapon
; :property.type/spell

; => misc ? species ?

; ASSERT & LOAD EDN / WRITE EDN / BEFORE SAVE DATA
; also things like target-entity props should be a map , :hit-effect & :maxrange, it was a list...

(defn property-type [props]
  (some (fn [[prop-type k]] (when (k props) prop-type))
        prop-type-unique-key))

(extend-type gdl.context.Context
  cdq.context/PropertyStore
  (get-property [{:keys [context/properties]} id]
    (safe-get properties id))

  (all-properties [{:keys [context/properties]} type]
    (filter (prop-type-unique-key type) (vals properties))))

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
  (sort-by
   (fn [prop]
     (let [ptype (property-type prop)]
       (case ptype
        :skill 0
        :creature 1
        :species 2
        :item 3
        :weapon 4
        9)))
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
