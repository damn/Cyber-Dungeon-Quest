(ns cdq.screens.game
  (:require [gdl.app :refer [current-context change-screen!]]
            [gdl.context :refer [get-stage render-world-view delta-time draw-text key-just-pressed? key-pressed? ->color render-tiled-map]]
            [gdl.screen :refer [Screen]]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.input.keys :as input.keys]
            [gdl.scene2d.actor :refer [visible? set-visible! toggle-visible!]]
            [gdl.scene2d.group :refer [children]]
            [utils.core :refer [safe-get]]
            [cdq.context :refer [render-entities* ray-blocked? explored? set-explored! line-of-sight? content-grid
                                  tick-entity! remove-destroyed-entities! update-mouseover-entity! update-potential-fields!
                                  update-elapsed-game-time! debug-render-after-entities debug-render-before-entities set-cursor!
                                  transact-all!]]
            [cdq.entity.movement :as movement]
            [cdq.entity.state :as state]
            cdq.context.ui.actors
            [cdq.world.content-grid :refer [active-entities]]))

(declare ^:private explored-tile-color)

(defn- init-explored-tile-color [ctx]
  (.bindRoot #'explored-tile-color (->color ctx 0.5 0.5 0.5 1)))

; TODO performance - need to deref current-context at every tile corner !!
; => see with prformance check later
; => need to pass to orthogonaltiledmap bla
; or pass only necessary data structures  (explored grid)
;=> the rays are more of a problem after sampling visualvm
(defn- tile-color-setter [_ x y]
  (let [{:keys [world-camera] :as context} @current-context
        light-position (camera/position world-camera) ; == player position use ?
        position [x y]
        explored? (explored? context position)
        base-color (if explored?
                     explored-tile-color
                     color/black)
        blocked? (ray-blocked? context light-position position)]
    (if blocked?
      base-color ; color/white TODO here set view all tiles debug
      (do
       (when-not explored?
         (set-explored! context position))
       color/white))))

; for now a function, see gdl.backends.libgdx.context.input reload bug
; otherwise keys in dev mode may be unbound because dependency order not reflected
; because bind-roots
(defn- hotkey->window []
  {input.keys/i :inventory-window
   input.keys/q :skill-window ; 's' moves also ! (WASD)
   input.keys/e :entity-info-window
   input.keys/h :help-window
   input.keys/z :debug-window})

(defn- check-window-hotkeys [context group]
  (doseq [[hotkey window] (hotkey->window)
          :when (key-just-pressed? context hotkey)]
    (toggle-visible! (get group window))))

(defn- adjust-zoom [camera by] ; DRY map editor
  (camera/set-zoom! camera (max 0.1 (+ (camera/zoom camera) by))))

(def ^:private zoom-speed 0.05)

(defn- end-of-frame-checks! [{:keys [context/config world-camera] :as context}]
  (when (key-pressed? context input.keys/shift-left)
    (adjust-zoom world-camera  zoom-speed))

  (when (key-pressed? context input.keys/minus)
    (adjust-zoom world-camera (- zoom-speed)))

  (let [group (:windows (get-stage context))
        windows (children group)]
    (when (safe-get config :debug-windows?)
      (check-window-hotkeys context group))

    (when (key-just-pressed? context input.keys/escape)
      (cond (some visible? windows) (run! #(set-visible! % false) windows)
            :else (change-screen! :screens/options-menu))))

  (when (key-just-pressed? context input.keys/tab)
    (change-screen! :screens/minimap)))

(defn- render-game [{:keys [context/world-map
                            context/player-entity
                            world-camera]
                     :as context}
                    active-entities]
  (camera/set-position! world-camera (:entity/position @player-entity))
  (render-tiled-map context
                    (:tiled-map world-map)
                    tile-color-setter)
  (render-world-view context
                     (fn [context]
                       (debug-render-before-entities context)
                       (render-entities* context
                                         (->> active-entities
                                              (map deref)
                                              (filter :entity/z-order)
                                              (filter #(line-of-sight? context @player-entity %)))) ; TODO here debug los disable
                       (debug-render-after-entities context))))

(def ^:private pausing? true)

(defn- assoc-delta-time [ctx]
  (assoc ctx :context/delta-time (min (delta-time ctx)
                                      movement/max-delta-time)))

(defn- update-game [{:keys [context/player-entity
                            context/game-paused?
                            cdq.context.ecs/thrown-error]
                     :as ctx}
                    active-entities]
  (let [state (:state-obj (:entity/state @player-entity))
        _ (transact-all! ctx (state/manual-tick state ctx))
        paused? (reset! game-paused? (or @thrown-error
                                         (and pausing? (state/pause-game? state))))
        ctx (assoc-delta-time ctx)]
    ; this do always so can get debug info even when game not running
    (update-mouseover-entity! ctx)
    (when (or (not paused?)
              (key-just-pressed? ctx input.keys/p))
      (update-elapsed-game-time! ctx)
      ; sowieso keine bewegungen / kein update gemacht ? checkt nur tiles ?
      (update-potential-fields! ctx active-entities)
      (doseq [entity active-entities]
        (tick-entity! ctx entity)))
    ; do not pause this as for example pickup item, should be destroyed.
    (remove-destroyed-entities! ctx)
    (end-of-frame-checks! ctx)))

(defrecord SubScreen []
  Screen
  (show [_ _context])

  (hide [_ ctx]
    (set-cursor! ctx :cursors/default))

  (render [_ {:keys [context/player-entity] :as context}]
    (let [active-entities (active-entities (content-grid context) player-entity)]
      (render-game context active-entities)
      (update-game context active-entities))))

(defn screen [context]
  (init-explored-tile-color context)
  {:actors (cdq.context.ui.actors/->ui-actors context)
   :sub-screen (->SubScreen)})
