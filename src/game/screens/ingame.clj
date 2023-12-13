(ns game.screens.ingame
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.lifecycle :as lc]
            [gdl.maps.tiled :as tiled]
            [gdl.math.vector :as v]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui :as ui]
            [game.properties :as properties]
            [game.ui.debug-window :as debug-window]
            [game.ui.help-window :as help-window]
            [game.ui.entity-info-window :as entity-info-window]
            [game.ui.skill-window :as skill-window]
            [game.ui.inventory-window :as inventory]
            [game.ui.action-bar :as action-bar]
            [game.ui.mouseover-entity :refer (get-mouseover-entity saved-mouseover-entity)]
            [game.components.clickable :as clickable]
            [game.components.hp :refer (dead?)]
            [game.components.skills :as skill-component]
            [game.maps.data :refer [get-current-map-data]]
            [game.player.entity :refer (player-entity)]
            [game.utils.lightning :refer [tile-color-setter]]
            game.update-ingame
            game.render-ingame)
  (:import (com.badlogic.gdx Gdx Input$Keys Input$Buttons)
           (com.badlogic.gdx.scenes.scene2d Actor Stage)))

(defn- item-on-cursor-render-actor []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (let [{:keys [drawer gui-mouse-position] :as context} (app/current-context)]
        (when-let [item (:item-on-cursor @player-entity)]
          ; windows keep changing z-index when selected, or put all windows in 1 group and this actor another group
          (.toFront ^Actor this)
          (draw/centered-image drawer
                               (:image item)
                               gui-mouse-position))))))

(defn- create-stage [{:keys [gui-viewport batch gui-viewport-width gui-viewport-height]}]
  (let [^Actor debug-window (debug-window/create)
        ^Actor help-window (help-window/create)
        ^Actor entity-info-window (entity-info-window/create)
        skill-window (skill-window/create)
        windows [debug-window
                 help-window
                 entity-info-window
                 inventory/window
                 skill-window]
        stage (stage/create gui-viewport batch)
        table (ui/table :rows [[{:actor action-bar/horizontal-group :expand? true :bottom? true}]]
                        :fill-parent? true)]
    (.addActor stage table)
    (.setPosition debug-window 0 gui-viewport-height)
    (.setPosition help-window
                  (- (/ gui-viewport-width 2)
                     (/ (.getWidth help-window) 2))
                  gui-viewport-height)
    (.setPosition inventory/window
                  gui-viewport-width
                  (- (/ gui-viewport-height 2)
                     (/ (.getHeight help-window) 2)))
    (.setPosition entity-info-window (.getX inventory/window) 0)
    (.setWidth entity-info-window (.getWidth inventory/window))
    (.setHeight entity-info-window (.getY inventory/window))
    (doseq [window windows]
      (.addActor stage window))
    (.addActor stage (item-on-cursor-render-actor))
    stage))

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

(defn- set-movement! [v]
  (swap! player-entity assoc :movement-vector v))

(defn- handle-key-input [{:keys [debug-window
                                 inventory-window
                                 entity-info-window
                                 skill-window
                                 help-window] :as stage}
                         {:keys [assets gui-mouse-position world-mouse-position]}]
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
                                (app/set-screen :game.screens.main))
       :else (app/set-screen :game.screens.options))))
  (when (.isKeyJustPressed Gdx/input Input$Keys/TAB)
    (app/set-screen :game.screens.minimap)) ; TODO does set-screen do it immediately (cancel the current frame ) or finish this frame?
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
    (set-movement! nil)
    (do
     (set-movement! (wasd-movement-vector))
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
           (clickable/clickable-mouseover-entity? (get-mouseover-entity)))
      (clickable/on-clicked {:stage stage
                             :assets assets}
                            (get-mouseover-entity))

      (saved-mouseover-entity) ; saved=holding leftmouse down after clicking on mouseover entity
      (set-movement! (v/direction (:position @player-entity)
                                  (:position @(saved-mouseover-entity))))

      (and (.isButtonPressed Gdx/input Input$Buttons/LEFT)
           (not (stage/hit stage gui-mouse-position)))
      (set-movement! (v/direction (:position @player-entity)
                                  world-mouse-position))))))

(defmethod skill-component/choose-skill :player [entity*]
  (when-let [skill-id @action-bar/selected-skill-id]
    ; TODO no skill selected and leftmouse -> also show msg to player/sound
    (when (and (not (:item-on-cursor entity*))
               (not (clickable/clickable-mouseover-entity? (get-mouseover-entity)))
               (or (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
                   (.isButtonPressed Gdx/input Input$Buttons/LEFT)))
      ; TODO directly pass skill here ...
      ; TODO should get from entity :skills ! not properties ... ?
      (let [state (skill-component/usable-state entity* (properties/get skill-id))]
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


(defmodule ^Stage stage
  (lc/create [_ context]
    (create-stage context))
  (lc/dispose [_]
    (.dispose stage))
  (lc/show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ context]
    (tiled/render-map context
                      (:tiled-map (get-current-map-data))
                      #'tile-color-setter)
    (app/render-with context
                     :world
                     #(game.render-ingame/render-map-content % context))
    (app/render-with context
                     :gui
                     #(game.render-ingame/render-gui % context))
    (.draw stage))
  (lc/tick [_ context delta]
    (handle-key-input stage context)
    (.act stage delta)
    (game.update-ingame/update-game context stage delta)))
