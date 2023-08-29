(nsx game.screens.ingame
  (:require [game.ui.debug-window :as debug-window]
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
            game.render-ingame)
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- item-in-hand-render-actor []
  (let [actor (proxy [Actor] []
                (draw [_ _]
                  (when @item-in-hand
                    (.toFront this) ; windows keep changing z-index when selected, or put all windows in 1 group and this actor another group
                    (image/draw-centered (:image @item-in-hand) (gui/mouse-position)))))]
    (ui/set-id actor :item-in-hand)
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
        stage (ui/stage)
        table (doto (ui/table)
                (.setFillParent true))]
    (.addActor stage table) ; stage/add-actor
    (ui/set-position debug-window 0 (gui/viewport-height))
    (ui/set-position help-window
                     (- (/ (gui/viewport-width) 2)
                        (/ (.getWidth help-window) 2)) ; actor/width
                     (gui/viewport-height))
    (ui/set-position inventory/window
                     (gui/viewport-width)
                     (- (/ (gui/viewport-height) 2)
                        (/ (.getHeight help-window) 2))) ; actor/height
    (ui/set-position inventory/window
                     (gui/viewport-width)
                     (- (/ (gui/viewport-height) 2)
                        (/ (.getHeight help-window) 2)))
    (ui/set-position entity-info-window
                     (.getX inventory/window) ; actor/x
                     0)
    ; actor/set-width ? or widget/?
    ; actor/set-height
    (.setWidth  entity-info-window (.getWidth inventory/window)) ; 333, 208
    (.setHeight entity-info-window (.getY inventory/window))
    ; => instead of add just
    ; add map
    ; or vector with id
    ; stage pass list of thingy?
    (doseq [window windows]
      (.addActor stage window)  ; move to positioning up
      (.setVisible window true) ; already visible?

      )
    ; TODO action-bar just place manually ?!
    (-> table ; table constructor here?
        (.add action-bar/horizontal-group) ; table/add
        .expand ; cell/expand
        .bottom) ; cell/bottom ?
    (.addActor stage (item-in-hand-render-actor))
    stage))

(defn- toggle-visible! [actor] ; TODO to ui/, no '!'
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
       (some ui/visible? windows) (run! ui/set-invisible windows)
       (dead? @player-entity) (if-not false ;#_(try-revive-player)
                                (app/set-screen :game.screens.main))
       :else (app/set-screen :game.screens.options))))
  (when (input/is-key-pressed? :TAB)
    (app/set-screen :game.screens.minimap)) ; TODO does set-screen do it immediately (cancel the current frame ) or finish this frame?
  ; TODO entity/skill info also
  (when (input/is-key-pressed? :I)
    (toggle-visible! inventory-window)
    (toggle-visible! entity-info-window)
    (toggle-visible! skill-window))

  (when (input/is-key-pressed? :H)
    (toggle-visible! help-window))

  (when (input/is-key-pressed? :Z)
    (toggle-visible! debug-window))

  ; we check left-mouse-pressed? and not left-mouse-down? because down may miss
  ; short taps between frames
  (if (:active-skill? @player-entity)
    (set-movement! nil)
    (do
     (set-movement! (wasd-movement-vector))
     (cond
      (and (input/is-leftm-pressed?)
           (not (ui/mouseover? stage))
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
           (not (ui/mouseover? stage))
           (clickable/check-clickable-mouseoverbody stage))
      nil

      (saved-mouseover-entity) ; saved=holding leftmouse down after clicking on mouseover entity
      (set-movement! (v/direction (:position @player-entity)
                                  (:position @(saved-mouseover-entity))))

      (and (input/is-leftbutton-down?)
           (not (ui/mouseover? stage)))
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
    (gui/render #(ui/draw-stage stage)))
  (lc/tick [_ delta]
    (handle-key-input stage)
    (ui/update-stage stage delta)
    (game.update-ingame/update-game stage delta)))
