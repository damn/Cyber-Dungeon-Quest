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

(defn- load-edn [file]
  (let [properties (-> file slurp edn/read-string)]
    (assert (apply distinct? (map :id properties)))
    (->> properties
         (map #(if (:image %)
                 (update % :image deserialize-image)
                 %))
         (#(zipmap (map :id %) %)))))

(declare ^:private properties)

(defmodule file
  (lc/create [_]
    (.bindRoot #'properties (load-edn file))))

(defn get [id]
  (safe-get properties id))

(defn all-with-key [k]
  (filter k (vals properties)))
