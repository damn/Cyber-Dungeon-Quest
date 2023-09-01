(ns game.screens.ingame
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.lc :as lc]
            [gdl.input :as input]
            [gdl.vector :as v]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui :as ui]
            [gdl.graphics.batch :refer [batch]]
            [gdl.graphics.image :as image]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.world :as world]
            [game.ui.debug-window :as debug-window]
            [game.ui.help-window :as help-window]
            [game.ui.entity-info-window :as entity-info-window]
            [game.ui.skill-window :as skill-window]
            [game.ui.inventory-window :as inventory]
            [game.ui.action-bar :as action-bar]
            [game.ui.mouseover-entity :refer (saved-mouseover-entity)]
            [game.components.clickable :as clickable]
            [game.components.hp :refer (dead?)]
            [game.components.inventory :refer [item-in-hand is-item-in-hand?]]
            [game.maps.data :as maps-data]
            [game.player.entity :refer (player-entity)]
            game.update-ingame
            game.render-ingame))

(defn- item-in-hand-render-actor []
  (let [actor (actor/create :draw
                            (fn [this]
                              (when @item-in-hand
                                (.toFront ^com.badlogic.gdx.scenes.scene2d.Actor this) ; windows keep changing z-index when selected, or put all windows in 1 group and this actor another group
                                (image/draw-centered (:image @item-in-hand) (gui/mouse-position)))))]
    actor))

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
                           (/ (actor/width help-window) 2)) ; actor/width
                        (gui/viewport-height))
    (actor/set-position inventory/window
                        (gui/viewport-width)
                        (- (/ (gui/viewport-height) 2)
                           (/ (actor/height help-window) 2))) ; actor/height
    (actor/set-position inventory/window
                        (gui/viewport-width)
                        (- (/ (gui/viewport-height) 2)
                           (/ (actor/height help-window) 2)))
    (actor/set-position entity-info-window
                        (.getX inventory/window) ; actor/x
                        0)
    ; actor/set-width ? or widget/?
    ; actor/set-height
    (actor/set-width  entity-info-window (actor/width inventory/window)) ; 333, 208
    (actor/set-height entity-info-window (.getY inventory/window))
    ; => instead of add just
    ; add map
    ; or vector with id
    ; stage pass list of thingy?
    (doseq [window windows]
      (stage/add-actor stage window)  ; move to positioning up
      (actor/set-visible window true) ; already visible?

      )
    (stage/add-actor stage (item-in-hand-render-actor))
    stage))

#_(def ^:private keybindings
  {:exit :ESCAPE
   :move-left :A
   :move-right :D

   }
  )

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

; TODO
; * show mouse cursor move/attack/etc depending on mouseoverentity or not
; (or in gui, then normal cursor)
; -> use same code ?? 'decide-action' => action -> cursor
; action -> function & curso
; if 'would press'

; TODO walk towards item if too far / also for melee range / or clicked on ground
; -> target position & pathfinding & block if blocked ??

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
           (clickable/check-clickable-mouseoverbody stage))
      nil

      (saved-mouseover-entity) ; saved=holding leftmouse down after clicking on mouseover entity
      (set-movement! (v/direction (:position @player-entity)
                                  (:position @(saved-mouseover-entity))))

      (and (input/is-leftbutton-down?)
           (not (stage/hit stage (gui/mouse-position))))
      (set-movement! (v/direction (:position @player-entity)
                                  (world/mouse-position)))))))

(import 'com.badlogic.gdx.graphics.Pixmap)
(import 'com.badlogic.gdx.Gdx)

; cursors ->
; * active-skill -> sanduhr
; * wasd movement -> ?
; * drop item
; * clickable (in range)
; * targeted movement
; * move (mouse outside GUI)  (highlight blocked tiles)
; * skill target/etc. (depends on selected skill)
; * inside GUI ?
; *  inventory take/etc. over cell

; TODO on window titlebar where you can move it !

; on entities where entity-info-window is available ?

; https://deburger.itch.io/fantasy-rpg-cursors-copper
; walk / etc / nice icons

; TODO active-skill should not allow interacting with GUI ?? how 2 do that.
; deactivate input?
; remove the inputprocessor/add it again ?
; but then there are also other actors non-gui related (update entity info etc. ?)
; just an extra input processor for only mouseclicks ...


; TODO items pickup as cursors !?

;(def ^:private cursors
;  {:drop-item
;   :clickable
;   :saved-mouseover-entity
;   :move
;   :inside-gui
;
;   }
;  )

; https://deburger.itch.io/fantasy-rpg-cursors-gold
#_(app/on-create
 (def cursor (Pixmap. (.internal (Gdx/files) "ui/cursors/001/0_red.png")))
 ; TODO do not forget to dispose
 ; https://libgdx.com/wiki/input/cursor-visibility-and-catching
 ; system cursors

 (def c (.newCursor g/graphics
                    cursor
                    0 0
                    ))

 (.setCursor (Gdx/graphics)
             c)

 ; Gdx.graphics.newCursor(pm,0,0,)
 ; Gdx.graphics.setCursor
 )

(defmodule stage
  (lc/create [_] (create-stage))
  ; TODO no dispose stage ? necessary ? no own batch? done at tiledmap renderer...
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_]
    (game.render-ingame/render-game)
    (gui/render #(stage/draw stage batch)))
  (lc/tick [_ delta]
    (handle-key-input stage)
    (stage/act stage delta)
    (game.update-ingame/update-game stage delta)))
