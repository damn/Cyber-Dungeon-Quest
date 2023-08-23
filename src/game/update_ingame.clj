(nsx game.update-ingame ; TODO game.tick
  (:require [clj-commons.pretty.repl :as p]
            [game.running :refer (running)]
            [game.utils.counter :refer :all]
            [game.player.entity :refer (player-entity)]
            [game.components.hp :refer (dead?)]
            [game.components.inventory :as inventory]
            [game.ui.mouseover-entity :refer (update-mouseover-entity)]
            game.components.update ; TODO namespace now only 2 modifiers.
            [game.utils.msg-to-player :refer [update-msg-to-player]]
            [game.maps.potential-field :refer (update-potential-fields)])
  (:use (game.maps [data :only (get-current-map-data)]
                   [mapchange :only (check-change-map)]
                   [contentfields :only (get-entities-in-active-content-fields)])
        [game.player.core :only (try-revive-player player-death)]))


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

; blocked / us => store in entity-.modifiers.

; => stun is also a modifier
; a modifier which renders/updates/removes/adds itself


; TODO == tick-e! tick-c / tick-e / tick-e! ? system = 'tick' only!

(defn- tick-c [{v 1 :as c} delta]
  (if-let [delta (delta? v delta)]
    (tick c delta)
    v))

(defn- tick-e [{v 1 :as c} e delta]
  (if-let [delta (delta? v delta)]
    (tick! c e delta)
    nil))

; no tick-e => if components working with each other, then it should be only 1 component !
; then move them together
; but tick! is always needed if we work with other entities
; TODO only those keys who are subscribed to the 'tick!' event !
; => (subscribed-keys tick! (keys @e))
; => could be cached per keyset-to-keyset comparison -> 2 keysets give a hash ?
; the hash could link to another keyset
; each 'keys' could be a hash number saved in the map
; do I need special extended maps ? maybe I dont think so
(defn- tick-entity! [e delta]
  (swap! e update-map tick-c delta)
  (doseq-entity e     tick-e delta))

; TODO if we work with datoms directly
; we have only 1 function/parameter :  [e a v]
; the function can return a transaction or just normal ??

; => I need something like [ doseq-map / for-map ]

(defn- tick-entities! [rs delta]
  (doseq [r rs]
    (tick-entity! r delta)))


; need to pass everything, either [c v] or [c v e] or [c v e r]
; the whole entity,attribute,value datom to the system !
; => there are 3 types of systems operating on datoms, entities, or with side effects
; knowing about the curren entity reference (r = eid)

; cv   system
; cve  system (not much used atm - or bad style ?)
; cver system
; => pass whole [cv or cver] to sys-application - fn
; so can check something with value in both cases
; but thats extremely complicated for 1 use case only (tick ! ) , see more use cases first!

; FUNNY ! IT SAYS SYSTEMS HERE IN THE NAME ~
(defn- update-game-systems [delta]
  ; destroy here not @ tick, because when game is paused
  ; for example pickup item, should be destroyed.
  (db/destroy-to-be-removed-entities!)
  (update-mouseover-entity) ; => a system
  (update-msg-to-player delta) ; => a system (but this is outside of pausing applicable) (should be part of update-stage !)
  (when @running
    (update-potential-fields))) ; => a system

; everything with 'state' (check session state)
; will be a component of the main 'game' entity ( & system)
; game = map of components !
; you can define a game just by data
; ultimate game language?
; pass systems I want or not / load components I want or not
; => !
;> all easy serialization ?
; functional possible then ?
; map component with tilemap

(defn- limit-delta [delta]
  (min delta game.components.body/max-delta))

(def thrown-error (atom nil))

(def ^:private pausing true)

; TODO stepping -> p is one step -> how to do ?

(defn update-game [delta]
  ;(reset! running false)

  (when (input/is-key-pressed? :P)
    (swap! running not))

  ; TODO do not set running to true in case of a throwable ('ERROR')
  ; TODO for deploying can run it anyway and just skip errors (only report 1 error per error not every update call)
  ; those updates/renders will then just not get rendered but the game will continue?

  ; TODO leftbutton in GUI handle ! all mouse presses / down handle check not processed by gui-stage
  (when pausing
    (if (and (not @thrown-error)
             (not (dead? @player-entity))
             (not (inventory/is-item-in-hand?)) ; do not run around w. item in hand

             ; == TODO =
             ; or active-skill?
             ; player-action?
             ; when stunned - ? paused or not ?
             (or (input/is-leftbutton-down?)
                 (:active-skill? @player-entity)
                 (:movement-vector @player-entity))) ; == WASD movement
      (reset! running true)
      (reset! running false)))

  (let [delta (limit-delta delta)]

    ; TODO all game systems must stop on pause
    ; if an error thrown there
    ; otherwise no need the wrap.
    (try (update-game-systems delta)
         (catch Throwable t
           (println "Catched throwable: ")
           (p/pretty-pst t)
           (reset! running false)
           (reset! thrown-error t) ; TODO show that an error appeared ! / or is paused without open debug window
           ))

    (when @running
      (try (tick-entities! (get-entities-in-active-content-fields)
                           delta)
           (catch Throwable t
             (println "Catched throwable: ")
             (p/pretty-pst t)
             (reset! running false)
             (reset! thrown-error t)))))

  (if (and @running
           (dead? @player-entity))
    ; TODO here reset-running false not @ player-death
    (player-death))

  (check-change-map))
