(ns game.screens.ingame
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.lc :as lc]
            [gdl.utils :refer [dispose]]
            [gdl.input :as input]
            [gdl.vector :as v]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui :as ui]
            [gdl.graphics.batch :refer [batch]]
            [gdl.graphics.image :as image]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.world :as world]
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
            [game.components.inventory :refer [item-in-hand is-item-in-hand?]]
            [game.components.skills :as skill-component]
            [game.maps.data :as maps-data]
            [game.player.entity :refer (player-entity)]
            game.update-ingame
            game.render-ingame))

(comment
 ; TODO => close button for windows
 (let [window (:inventory-window (gdl.app/current-screen-value))
       top (first (seq (.getChildren window)))
       ]
   (.add top (gdl.scene2d.ui/text-button "x" #(.setVisible window false)))
   )

 )

(defn- item-in-hand-render-actor []
  (actor/create :draw
                (fn [this]
                  (when @item-in-hand
                    ; windows keep changing z-index when selected, or put all windows in 1 group and this actor another group
                    (.toFront ^com.badlogic.gdx.scenes.scene2d.Actor this)
                    (image/draw-centered (:image @item-in-hand) (gui/mouse-position))))))

(defn- create-stage []
  (let [debug-window       (debug-window/create)
        help-window        (help-window/create)
        entity-info-window (entity-info-window/create)
        skill-window       (skill-window/create)
        windows [debug-window
                 help-window
                 entity-info-window
                 inventory/window
                 skill-window]
        stage (stage/create gui/viewport batch)
        table (ui/table :rows [[{:actor action-bar/horizontal-group :expand? true :bottom? true}]]
                        :fill-parent? true)]
    (stage/add-actor stage table)
    (actor/set-position debug-window 0 (gui/viewport-height))
    (actor/set-position help-window
                        (- (/ (gui/viewport-width) 2)
                           (/ (actor/width help-window) 2))
                        (gui/viewport-height))
    (actor/set-position inventory/window
                        (gui/viewport-width)
                        (- (/ (gui/viewport-height) 2)
                           (/ (actor/height help-window) 2)))
    (actor/set-position entity-info-window
                        (.getX inventory/window) ; actor/x
                        0)
    (actor/set-width  entity-info-window (actor/width inventory/window))
    (actor/set-height entity-info-window (.getY inventory/window))
    (doseq [window windows]
      (stage/add-actor stage window))
    (stage/add-actor stage (item-in-hand-render-actor))
    stage))

(defn- add-vs [vs]
  (v/normalise (reduce v/add [0 0] vs)))

(defn- wasd-movement-vector []
  (let [r (if (input/is-key-down? :D) [1  0])
        l (if (input/is-key-down? :A) [-1 0])
        u (if (input/is-key-down? :W) [0  1])
        d (if (input/is-key-down? :S) [0 -1])]
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
                                 help-window] :as stage}]
  (action-bar/up-skill-hotkeys)
  (let [windows [debug-window
                 help-window
                 entity-info-window
                 inventory-window
                 skill-window]]
    (when (input/is-key-pressed? :ESCAPE)
      (cond
       ; when game is paused and/or the player is dead, let player be able to drop item-in-hand?
       ; or drop it automatically when dead?
       ; need to drop it here else in options menu it is still item-in-hand at cursor!
       (is-item-in-hand?) (inventory/put-item-on-ground)
       (some actor/visible? windows) (run! actor/set-invisible windows)
       (dead? @player-entity) (if-not false ;#_(try-revive-player)
                                (app/set-screen :game.screens.main))
       :else (app/set-screen :game.screens.options))))
  (when (input/is-key-pressed? :TAB)
    (app/set-screen :game.screens.minimap)) ; TODO does set-screen do it immediately (cancel the current frame ) or finish this frame?
  ; TODO entity/skill info also
  (when (input/is-key-pressed? :I)
    (actor/toggle-visible inventory-window)
    (actor/toggle-visible entity-info-window)
    (actor/toggle-visible skill-window))

  (when (input/is-key-pressed? :H)
    (actor/toggle-visible help-window))

  (when (input/is-key-pressed? :Z)
    (actor/toggle-visible debug-window))

  ; we check left-mouse-pressed? and not left-mouse-down? because down may miss
  ; short taps between frames
  (if (:active-skill? @player-entity)
    (set-movement! nil)
    (do
     (set-movement! (wasd-movement-vector))
     (cond
      (and (input/is-leftm-pressed?)
           (not (stage/hit stage (gui/mouse-position)))
           (is-item-in-hand?))
      (inventory/put-item-on-ground)

      ; running around w. item in hand -> how is d2 doing this?
      ; -> do not run the game in this case (no action)
      ;(is-item-in-hand?)
      ;nil

      ; is-leftbutton-down? because hold & click on pressable -> move closer and in range click
      ; TODO is it possible pressed and not down ?
      (and (or (input/is-leftm-pressed?)
               (input/is-leftbutton-down?))
           (not (stage/hit stage (gui/mouse-position)))
           (clickable/clickable-mouseover-entity? (get-mouseover-entity)))
      (clickable/on-clicked stage (get-mouseover-entity))

      (saved-mouseover-entity) ; saved=holding leftmouse down after clicking on mouseover entity
      (set-movement! (v/direction (:position @player-entity)
                                  (:position @(saved-mouseover-entity))))

      (and (input/is-leftbutton-down?)
           (not (stage/hit stage (gui/mouse-position))))
      (set-movement! (v/direction (:position @player-entity)
                                  (world/mouse-position)))))))

(defmethod skill-component/choose-skill :player [entity]
  (when-let [skill-id @action-bar/selected-skill-id]
    (when (and skill-id ; not necessary !
               (not (is-item-in-hand?))
               (not (clickable/clickable-mouseover-entity? (get-mouseover-entity)))
               (or (input/is-leftm-pressed?)
                   (input/is-leftbutton-down?)))
      ; TODO directly pass skill here ...
      (let [usable? (skill-component/is-usable? (properties/get skill-id) player-entity)]
        (when usable?
          skill-id)))))

; TODO not enough mana/etc. show
; melee out of range shouldnt fire ...
; no move & shoot?
; TODO projectile needs target ...
; bat & meditation works
; melee does out of range already
; => ai-should-use? also passt for player ! checks in range or sth... hmm
; maybe better copy controls of another game, but I want the pause feature?
; TODO maybe no need 'skill-id' always ?
; copies are just pointers at immutable data structures...

(comment
 (skill-component/is-usable? (properties/get :projectile) player-entity))

(defmodule stage
  (lc/create [_] (create-stage))
  (lc/dispose [_] (dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_]
    (game.render-ingame/render-game)
    (gui/render #(stage/draw stage batch)))
  (lc/tick [_ delta]
    (handle-key-input stage)
    (stage/act stage delta)
    (game.update-ingame/update-game stage delta)))
