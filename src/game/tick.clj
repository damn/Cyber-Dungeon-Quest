(ns game.tick
  (:require [x.x :refer [defsystem update-map doseq-entity]]))

(defsystem tick  [c delta])
(defsystem tick! [c e delta])

; # Why do we use a :blocks counter and not a boolean?
; Different effects can stun/block for example :movement component
; and if we remove the effect, other effects still need to be there
; so each effect increases the :blocks-count by 1 and decreases them after the effect ends.
(defn- blocked? [v]
  (when-let [cnt (:blocks v)]
    (assert (and (integer? cnt)
                 (>= cnt 0)))
    (> cnt 0)))

(defn- delta-speed [delta v]
  (->> (or (:update-speed v) 1)
       (* delta)
       int
       (max 0)))

(defn- delta? [v delta]
  (if (blocked? v)
    nil
    (delta-speed delta v)))

(defn- tick-component [{v 1 :as c} delta]
  (if-let [delta (delta? v delta)]
    (tick c delta)
    v))

(defn- tick-entity* [{v 1 :as c} e delta]
  (if-let [delta (delta? v delta)]
    (tick! c e delta)
    nil))

(defn- tick-entity! [e delta]
  (swap! e update-map tick-component delta)
  (doseq-entity e tick-entity* delta))

(defn tick-entities! [rs delta]
  (doseq [r rs]
    (tick-entity! r delta)))
