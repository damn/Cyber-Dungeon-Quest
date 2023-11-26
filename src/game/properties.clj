(ns game.properties
  (:refer-clojure :exclude [get])
  (:require [clojure.edn :as edn]
            [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.graphics.image :as image]
            [utils.core :refer [safe-get]]))

; could just use sprite-idx directly.
(defn- deserialize-image [{:keys [file sub-image-bounds]}]
  {:pre [file sub-image-bounds]}
  (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
        [tilew tileh]       (drop 2 sub-image-bounds)]
    (image/get-sprite {:file file
                       :tilew tileh
                       :tileh tilew}
                      [(int (/ sprite-x tilew))
                       (int (/ sprite-y tileh))])))

(defn- serialize-image [image]
  (select-keys image [:file :sub-image-bounds]))

(comment
 (clojure.pprint/pprint
  (media/fx-impact-animation [3 0]))

 ; select-keys and map serialize-image frames / frame-duration
 ; or map deserialize image / ...
 ; for :animation key ....

 )

(defn- load-edn [file]
  (let [properties (-> file slurp edn/read-string)]
    (assert (apply distinct? (map :id properties)))
    (->> properties
         (map #(if (:image %)
                 (update % :image deserialize-image)
                 %))
         (#(zipmap (map :id %) %)))))

(declare ^:private properties-file
         ^:private properties)

(defmodule file
  (lc/create [_]
    (.bindRoot #'properties-file file)
    (.bindRoot #'properties (load-edn file))))

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
       (map #(if (:image %)
               (update % :image serialize-image)
               %))
       (save-edn properties-file)))

(defn save! [data]
  {:pre [(contains? data :id)
         (contains? properties (:id data))
         (= (set (keys data)) (set (keys (get (:id data)))))]}
  (alter-var-root #'properties update (:id data) merge data)
  (save-all-properties!))

; TODO schema after load/before save
; => which keys?
; => also key properties
; => what of the properties with are first (not slot yet) ?
; => add resources/maps.edn
