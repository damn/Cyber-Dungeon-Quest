(ns game.tick
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [update-map doseq-entity]]
            [gdl.app :as app]
            [gdl.math.vector :as v]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [utils.core :refer [safe-get]]
            [game.context :as gm]
            [game.entity :as entity]
            [game.components.hp :refer (dead?)]
            [game.components.movement :as movement]
            [game.components.skills :as skill-component]
            [game.components.clickable :as clickable]
            [game.ui.action-bar :as action-bar]
            [game.ui.inventory-window :as inventory]
            [game.ui.mouseover-entity :refer (update-mouseover-entity
                                              get-mouseover-entity
                                              saved-mouseover-entity)]
            [game.utils.msg-to-player :refer [update-msg-to-player]]
            ;[game.maps.mapchange :refer [check-change-map]]
            [game.maps.contentfields :refer [get-entities-in-active-content-fields]]
            [game.maps.potential-field :refer [update-potential-fields]])
  (:import (com.badlogic.gdx Gdx Input$Keys Input$Buttons)
           com.badlogic.gdx.scenes.scene2d.Actor))

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

(defn- tick-entity! [context entity delta]
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
                      (entity/tick! [k v] context e delta)
                      nil))
                  delta)))

(defn- update-game-systems [{:keys [gui-mouse-position
                                    context/running]
                             :as context}
                            stage
                            delta]
  ; destroy here not @ tick, because when game is paused
  ; for example pickup item, should be destroyed.
  (gm/destroy-to-be-removed-entities! context)
  ; TODO or pass directly stage/hit boolean
  (update-mouseover-entity stage context)
  (update-msg-to-player delta)
  (when @running
    (update-potential-fields context)))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

(def thrown-error (atom nil))

(def ^:private pausing true)


(defn- add-vs [vs]
  (v/normalise (reduce v/add [0 0] vs)))

(defn- wasd-movement-vector []
  (let [r (if (.isKeyPressed Gdx/input Input$Keys/D) [1  0])
        l (if (.isKeyPressed Gdx/input Input$Keys/A) [-1 0])
        u (if (.isKeyPressed Gdx/input Input$Keys/W) [0  1])
        d (if (.isKeyPressed Gdx/input Input$Keys/S) [0 -1])]
    (when (or r l u d)
      (let [v (add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn- set-movement! [player-entity v]
  (swap! player-entity assoc :movement-vector v))

(defn- handle-key-input [{:keys [debug-window
                                 inventory-window
                                 entity-info-window
                                 skill-window
                                 help-window] :as stage}
                         {:keys [assets gui-mouse-position world-mouse-position context/player-entity]}]
  (action-bar/up-skill-hotkeys)
  (let [windows [debug-window
                 help-window
                 entity-info-window
                 inventory-window
                 skill-window]]
    (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
      (cond
       ; when game is paused and/or the player is dead, let player be able to drop item-on-cursor?
       ; or drop it automatically when dead?
       (:item-on-cursor @player-entity) (inventory/put-item-on-ground {:assets assets})
       (some #(.isVisible ^Actor %) windows) (run! #(.setVisible ^Actor % false) windows)
       (dead? @player-entity) (if-not false
                                (app/change-screen! :screens/main-menu))
       :else (app/change-screen! :screens/options-menu))))
  (when (.isKeyJustPressed Gdx/input Input$Keys/TAB)
    (app/change-screen! :screens/minimap)) ; TODO does  do it immediately (cancel the current frame ) or finish this frame?
  ; TODO entity/skill info also
  (when (.isKeyJustPressed Gdx/input Input$Keys/I)
    (actor/toggle-visible! inventory-window)
    (actor/toggle-visible! entity-info-window)
    (actor/toggle-visible! skill-window))

  (when (.isKeyJustPressed Gdx/input Input$Keys/H)
    (actor/toggle-visible! help-window))

  (when (.isKeyJustPressed Gdx/input Input$Keys/Z)
    (actor/toggle-visible! debug-window))

  ; we check left-mouse-pressed? and not left-mouse-down? because down may miss
  ; short taps between frames
  (if (:active-skill? @player-entity)
    (set-movement! player-entity nil)
    (do
     (set-movement! player-entity (wasd-movement-vector))
     (cond
      (and (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
           (not (stage/hit stage gui-mouse-position))
           (:item-on-cursor @player-entity))
      (inventory/put-item-on-ground {:assets assets})

      ; running around w. item in hand -> how is d2 doing this?
      ; -> do not run the game in this case (no action)
      ;(:item-on-cursor @player-entity)
      ;nil

      ; is-leftbutton-down? because hold & click on pressable -> move closer and in range click
      ; TODO is it possible pressed and not down ?
      (and (or (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
               (.isButtonPressed Gdx/input Input$Buttons/LEFT))
           (not (stage/hit stage gui-mouse-position))
           (clickable/clickable-mouseover-entity? @player-entity
                                                  (get-mouseover-entity)))
      (clickable/on-clicked {:stage stage
                             :assets assets}
                            (get-mouseover-entity))

      (saved-mouseover-entity) ; saved=holding leftmouse down after clicking on mouseover entity
      (set-movement! player-entity (v/direction (:position @player-entity)
                                                (:position @(saved-mouseover-entity))))

      (and (.isButtonPressed Gdx/input Input$Buttons/LEFT)
           (not (stage/hit stage gui-mouse-position)))
      (set-movement! player-entity (v/direction (:position @player-entity)
                                                world-mouse-position))))))

(defmethod skill-component/choose-skill :player [{:keys [context/properties]} entity*]
  (when-let [skill-id @action-bar/selected-skill-id]
    ; TODO no skill selected and leftmouse -> also show msg to player/sound
    (when (and (not (:item-on-cursor entity*))
               (not (clickable/clickable-mouseover-entity? entity*
                                                           (get-mouseover-entity)))
               (or (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
                   (.isButtonPressed     Gdx/input Input$Buttons/LEFT)))
      ; TODO directly pass skill here ...
      ; TODO should get from entity :skills ! not properties ... ?
      (let [state (skill-component/usable-state entity* (safe-get properties skill-id))]
        (if (= state :usable)
          skill-id
          #_(println (str "Skill usable state not usable: " state))
          )))))

(comment
 [game.utils.msg-to-player :refer (show-msg-to-player)]

 (defn- denied [text]
  ; (.play ^Sound (get assets asset-manager "bfxr_denied.wav"))
  ; deactivated because on mouse down sound gets played again every frame
  ; would need to use Music class instead of Sound for checking isPlaying already
  ; and playing only once
  ; -> could make a timer if message is the same, dont send sound( /msg to player )
  (show-msg-to-player text)))

; TODO not enough mana/etc. show REASON for skill not usable ! (or not useful !)
; melee out of range shouldnt fire ...
; no move & shoot?
; TODO projectile needs target ...
; bat & meditation works
; melee does out of range already
; => ai-should-use? also passt for player ! checks in range or sth... hmm (maybe 'useful?')
; maybe better copy controls of another game, but I want the pause feature?
; TODO maybe no need 'skill-id' always ?
; copies are just pointers at immutable data structures...

; TODO stepping -> p is one step -> how to do ?

(defn tick-game [{:keys [context/player-entity
                           context/running]
                    :as context}
                   stage
                   delta]
  (handle-key-input stage context)
  ;(reset! running false)
  (when (.isKeyJustPressed Gdx/input Input$Keys/P)
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
             (or (.isButtonPressed Gdx/input Input$Buttons/LEFT)
                 (:active-skill? @player-entity)
                 (:movement-vector @player-entity))) ; == WASD movement
      (reset! running true)
      (reset! running false)))
  (let [delta (limit-delta delta)]
    ; TODO all game systems must stop on pause
    ; if an error thrown there
    ; otherwise no need the wrap.
    (try (update-game-systems context stage delta)
         (catch Throwable t
           (println "Catched throwable: ")
           (p/pretty-pst t)
           (reset! running false)
           (reset! thrown-error t) ; TODO show that an error appeared ! / or is paused without open debug window
           ))
    (when @running
      (try (doseq [entity (get-entities-in-active-content-fields context)]
             (tick-entity! context entity delta))
           (catch Throwable t
             (println "Catched throwable: ")
             (p/pretty-pst t)
             (reset! running false)
             (reset! thrown-error t)))))
  (when (and @running
             (dead? @player-entity))
    (reset! running false)
    (gm/play-sound! context "sounds/bfxr_playerdeath.wav"))
  #_(check-change-map))
