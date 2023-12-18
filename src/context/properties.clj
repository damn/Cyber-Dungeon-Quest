(ns context.properties
  (:refer-clojure :exclude [get])
  (:require [clojure.edn :as edn]
            [gdl.graphics.animation :as animation]
            [gdl.protocols :refer [get-sprite]]
            [utils.core :refer [safe-get]]))

; Other approaches :
; multimethod & postwalk like cdq & use records ... or metadata hmmm , but then have these records there with nil fields etc.
; print-dup prints weird stuff like #Float 0.5
; print-method fucks up console printing, would have to add methods and remove methods during save/load
; => simplest way: just define keys which are assets (which are all the same anyway at the moment)

; TODO
; 1. simply add for :sound / :animation serialization/deserializeation like image
; 2. add :property/type required attribute which leads to clearly defined schema/specs which are checked etc..
; 3. add spec validation on load, save, change, make it work .
; 4. add other hardcoded stuff like projectiles, etc.

; could just use sprite-idx directly?
(defn- deserialize-image [context {:keys [file sub-image-bounds]}]
  {:pre [file sub-image-bounds]}
  (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
        [tilew tileh]       (drop 2 sub-image-bounds)]
    ; TODO is not the record itself, check how to do @ image itself.
    (get-sprite context
                {:file file
                 :tilew tileh
                 :tileh tilew}
                [(int (/ sprite-x tilew))
                 (int (/ sprite-y tileh))])))

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
       (#(if (:image %)
           (update % :image (fn [img] (deserialize-image context img)))
           %))
       (#(if (:animation %)
           (update % :animation (fn [anim] (deserialize-animation context anim)))
           %))))

(defn- serialize [data]
  (->> data
       (#(if (:image     %) (update % :image     serialize-image)     %))
       (#(if (:animation %) (update % :animation serialize-animation) %))))

(defn load-edn [context file]
  ; TODO use gdx internal files -> context function -> no 'resources/' necessary
  (let [properties (-> file slurp edn/read-string)]
    (assert (apply distinct? (map :id properties)))
    (->> properties
         (map #(deserialize context %))
         (#(zipmap (map :id %) %)))))

(declare properties-file
         properties)

(defn get [id]
  (safe-get properties id))

; TODO new type => add data here
(def ^:private prop-type-unique-key
  {:species :hp
   :creature :species
   :item :slot
   :skill :effect
   ; TODO spells => only part skills with spell? ....
   ; its more like 'views' not fixed exclusive types
   :weapon (fn [{:keys [slot]}] (and slot (= slot :weapon)))})

(defn property-type [props]
  (some (fn [[prop-type k]] (when (k props) prop-type))
        prop-type-unique-key))

(defn get-all [property-type]
  (filter (prop-type-unique-key property-type) (vals properties)))

(defn- save-edn [file data]
  (binding [*print-level* nil]
    (spit file
          (with-out-str
           (clojure.pprint/pprint
            data)))))

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

(defn- save-all-properties! []
  (->> properties
       vals
       sort-by-type
       (map serialize)
       (save-edn properties-file)))

(defn save! [data]
  {:pre [(contains? data :id)
         ; comment next 2 lines to add new properties with a new id
         (contains? properties (:id data))
         ; TODO this get uses defined properties.get, unclear
         (= (set (keys data)) (set (keys (get (:id data)))))
         ]}
  (alter-var-root #'properties update (:id data) merge data)
  (save-all-properties!))
