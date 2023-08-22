; TODO ns+ => all gdl requires
(nsx game.screens.ingame ; => game.screens.game
  (:require [game.ui.stage :as stage]
            [game.ui.debug :as debug-window]
            [game.ui.help-window :as help-window]
            [game.ui.entity-info :as entity-info]
            [game.ui.skill-menu :as skill-menu]
            [game.ui.action-bar :as action-bar]
            [game.ui.mouseover-entity :refer (saved-mouseover-entity)]

            [game.components.clickable :as clickable]
            [game.components.hp :refer (dead?)]

            [game.maps.data :as maps-data]

            [game.items.inventory :as inventory]

            [game.player.entity :refer (player-entity)]

            game.update-ingame
            game.render-ingame)
  (:import [com.badlogic.gdx InputAdapter InputMultiplexer]))

; TODO private inside not top level def doesnt work.
(app/on-create
 ; TODO maybe call functions here which create that -> no need to defmanage everything
 ; everywhere
 ; => 1 app/defmanaged gui-stage ?
 (def ^:private windows [debug-window/window
                         help-window/window
                         entity-info/window
                         inventory/window
                         skill-menu/window])

 ; TODO skill-menu rebuild at new session
 ; -> session lifecycle not create/destroy lifecycle

 ; TODO put hotkey text in title
 #_(.setText (.getTitleLabel skill-menu/window)
           "foo"
           )

 (.setPosition debug-window/window 0 (gui/viewport-height))

 (.setPosition help-window/window
               (- (/ (gui/viewport-width) 2)
                  (/ (.getWidth help-window/window) 2))
               (gui/viewport-height))

 (.setPosition inventory/window
               (gui/viewport-width)
               (- (/ (gui/viewport-height) 2)
                  (/ (.getHeight help-window/window) 2)))

 (.setPosition inventory/window
               (gui/viewport-width)
               (- (/ (gui/viewport-height) 2)
                  (/ (.getHeight help-window/window) 2)))

 (.setPosition entity-info/window
               (.getX inventory/window)
               0)

 (.setWidth  entity-info/window (.getWidth inventory/window)) ; 333, 208
 (.setHeight entity-info/window (.getY inventory/window))

 (doseq [window windows]
   (.addActor stage/stage window) ; -> ui/add -> Addable Interface ?!...
   (.setVisible window true))

 (-> stage/table
     (.add action-bar/horizontal-group)
     .expand
     .bottom))

(defn- toggle-visible! [actor]
  (.setVisible actor (not (.isVisible actor))))

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

(defn- handle-key-input []

  (action-bar/up-skill-hotkeys)

  (when (input/is-key-pressed? :ESCAPE)
    (cond
     ; when game is paused and/or the player is dead, let player be able to drop item-in-hand?
     ; or drop it automatically when dead?
     ; need to drop it here else in options menu it is still item-in-hand at cursor!
     (inventory/is-item-in-hand?) (inventory/put-item-on-ground)

     (some (memfn isVisible) windows) (dorun (map #(.setVisible % false) windows))

     (dead? @player-entity) (if-not false ;#_(try-revive-player)
                                 (gdl.app/set-screen :game.screens.main))

     :else (gdl.app/set-screen :game.screens.options)))

  (when (input/is-key-pressed? :TAB)
    (gdl.app/set-screen :game.screens.minimap))

  ; TODO entity/skill info also

  (when (input/is-key-pressed? :I)
    (toggle-visible! inventory/window)
    (toggle-visible! entity-info/window)
    (toggle-visible! skill-menu/window))

  (when (input/is-key-pressed? :H)
    (toggle-visible! help-window/window))

  (when (input/is-key-pressed? :Z)
    (toggle-visible! debug-window/window))

  ; we check left-mouse-pressed? and not left-mouse-down? because down may miss
  ; short taps between frames
  (if (:active-skill? @player-entity)
    (set-movement! nil)
    (do
     (set-movement! (wasd-movement-vector))
     (cond
      (and (input/is-leftm-pressed?)
           (not (stage/mouseover-gui?))
           (inventory/is-item-in-hand?))
      (inventory/put-item-on-ground)

      ; running around w. item in hand -> how is d2 doing this?
      ; -> do not run the game in this case (no action)
      ;(inventory/is-item-in-hand?)
      ;nil

      ; is-leftbutton-down? because hold & click on pressable -> move closer and in range click
      ; TODO is it possible pressed and not down ?
      (and (or (input/is-leftm-pressed?)
               (input/is-leftbutton-down?))
           (not (stage/mouseover-gui?))
           (clickable/check-clickable-mouseoverbody))
      nil

      (saved-mouseover-entity) ; saved=holding leftmouse down after clicking on mouseover entity
      (set-movement! (v/direction (:position @player-entity)
                                  (:position @(saved-mouseover-entity))))

      (and (input/is-leftbutton-down?)
           (not (stage/mouseover-gui?)))
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

(defmodule _
  (lc/show [_] (input/set-processor stage/stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_]
    (game.render-ingame/render-game)
    (gui/render (fn []
                  (ui/draw-stage stage/stage)
                  (inventory/render-item-in-hand-on-cursor))))
  (lc/tick [_ delta]
    (handle-key-input)
    (ui/update-stage stage/stage delta)
    (game.update-ingame/update-game delta)))
