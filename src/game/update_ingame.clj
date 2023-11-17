(ns game.update-ingame
  (:require [clj-commons.pretty.repl :as p]
            [gdl.input :as input]
            [game.running :refer (running)]
            [game.tick :as tick]
            [game.db :as db]
            [game.player.entity :refer (player-entity)]
            [game.components.hp :refer (dead?)]
            [game.components.inventory :as inventory]
            [game.ui.mouseover-entity :refer (update-mouseover-entity)]
            [game.components.movement :as movement]
            [game.player.core :refer [player-death]]
            [game.utils.msg-to-player :refer [update-msg-to-player]]
            [game.maps.mapchange :refer [check-change-map]]
            [game.maps.contentfields :refer [get-entities-in-active-content-fields]]
            [game.maps.potential-field :refer [update-potential-fields]]))

(defn- update-game-systems [stage delta]
  ; destroy here not @ tick, because when game is paused
  ; for example pickup item, should be destroyed.
  (db/destroy-to-be-removed-entities!)
  (update-mouseover-entity stage)
  (update-msg-to-player delta)
  (when @running
    (update-potential-fields)))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

(def thrown-error (atom nil))

(def ^:private pausing true)

; TODO stepping -> p is one step -> how to do ?

(defn update-game [stage delta]
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
             (not (:item-on-cursor @player-entity)) ; do not run around w. item in hand

             ; TODO animation/game runs when moving UI window around ....

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
    (try (update-game-systems stage delta)
         (catch Throwable t
           (println "Catched throwable: ")
           (p/pretty-pst t)
           (reset! running false)
           (reset! thrown-error t) ; TODO show that an error appeared ! / or is paused without open debug window
           ))

    (when @running
      (try (tick/tick-entities! (get-entities-in-active-content-fields)
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
