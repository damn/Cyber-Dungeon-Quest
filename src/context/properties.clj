(ns context.properties
  (:require [clojure.edn :as edn]
            [gdl.context :refer [get-sprite]]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get]]
            cdq.context))

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

(extend-type gdl.context.Context
  cdq.context/PropertyStore
  (get-property [{:keys [context/properties]} id]
    (safe-get properties id))
  (all-properties [{:keys [context/properties]} type]
    (filter (prop-type-unique-key type) (vals properties))))

(require 'gdl.backends.libgdx.context.image-drawer-creator)

; could just use sprite-idx directly?
(defn- deserialize-image [context {:keys [file sub-image-bounds]}]
  {:pre [file sub-image-bounds]}
  (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
        [tilew tileh]       (drop 2 sub-image-bounds)]
    ; TODO is not the image record itself, check how to do @ image itself.
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

; TODO serialize / deserialize protocol !??!
(defn- load-edn [context file]
  (let [properties (-> file slurp edn/read-string)] ; TODO use .internal Gdx/files  => part of context protocol
    (assert (apply distinct? (map :id properties)))
    (->> properties
         (map #(deserialize context %))
         (#(zipmap (map :id %) %)))))

(defn ->context [context file]
  {:context/properties (load-edn context file)
   :context/properties-file file})

; usage types:
; safe-get one property with id
; get all properties of a type
; 'get-property' ?
; or just 'property' ?

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
                                 {:keys [id] :as data}]
  {:pre [(contains? data :id)
         (contains? properties id)
         (= (set (keys data))
            (set (keys (get properties id))))]}
  (let [properties (update properties id merge data)]
    (write-to-file! properties properties-file)
    (assoc context :context/properties properties)))
