(ns game.tick
  (:require [x.x :refer [update-map doseq-entity]]
            [game.entity :as entity]))

; TODO blocks/update-speed /attack-speed/etc. into :modifiers component ?
; namespaced keywords for everything effect-params, components, etc. ...
; => easier to find

; # Why do we use a :blocks counter and not a boolean?
; Different effects can stun/block for example :movement component
; and if we remove the effect, other effects still need to be there
; so each effect increases the :blocks-count by 1 and decreases them after the effect ends.
(defn- blocked? [blocks k]
  (when-let [cnt (k blocks)]
    (assert (and (integer? cnt)
                 (>= cnt 0)))
    (> cnt 0)))

(defn- delta-speed [update-speeds k delta]
  (->> (or (get update-speeds k) 1)
       (* delta)
       int
       (max 0)))

(defn- delta? [modifiers k delta]
  (if (blocked? (:blocks modifiers) k)
    nil
    (delta-speed (:update-speed modifiers) k delta)))

(defn tick-entity! [entity delta]
  (let [modifiers (:modifiers @entity)]
    (swap! entity update-map
           (fn [[k v] delta]
             (if-let [delta (delta? modifiers k delta)]
               (entity/tick [k v] delta)
               v))
           delta)
    (doseq-entity entity
                  (fn [[k v] e delta]
                    (if-let [delta (delta? modifiers k delta)]
                      (entity/tick! [k v] e delta)
                      nil))
                  delta)))
