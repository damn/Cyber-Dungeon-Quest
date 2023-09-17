(ns game.maps.data
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.tiled :as tiled]
            [game.session :as session]))

(declare ^:private maps-data
         added-map-order)

(def state (reify session/State
               (load! [_ _]
                 (.bindRoot #'maps-data {})
                 (.bindRoot #'added-map-order []))
               (serialize [_])
               (initial-data [_])))

(defn add-map [k data]
  (alter-var-root #'maps-data assoc k data)
  (alter-var-root #'added-map-order conj k))

(defn get-map-keys []
  (keys maps-data))

(defn get-map-data [map-name]
  (get maps-data map-name))

(defn get-pretty-name [map-key]
  (:pretty-name (get-map-data map-key)))

; TODO faster access when current-map references maps-data and not the key?
; test get-cells for example ...

; 1 Do not change this while @update-components
; -> or all comps of one map using grids of other maps etc.
; 2 use only do-in-map or set-map!
; 3 only set to a existing map - key
; TODO VAR DOC?
(def current-map (atom nil))

(defn set-map! [new-map]
  {:pre [(contains? maps-data new-map)]}
  (reset! current-map new-map))

(defmacro do-in-map
  "Executes the exprs with current-map set to \"in\" and restores the previous current-map afterwards."
  [in & exprs]
  `(let [old# @current-map
         ~'_ (reset! current-map ~in)
         retrn# (do ~@exprs)]
     (reset! current-map old#)
     retrn#))

(defn get-current-map-data []
  (get maps-data @current-map))

(defmodule _
  (lc/dispose [_]
    (when (bound? #'maps-data)
      (doseq [[mapkey mapdata] maps-data
              :let [tiled-map (:tiled-map mapdata)]
              :when tiled-map]
        (tiled/dispose tiled-map)))))
