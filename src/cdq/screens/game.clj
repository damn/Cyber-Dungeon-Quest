(ns cdq.screens.game
  (:require [gdl.app :refer [change-screen!]]
            [gdl.context :refer [render-world-view delta-time key-just-pressed? key-pressed?]]
            [gdl.screen :refer [Screen]]
            [gdl.graphics.camera :as camera]
            [gdl.input.keys :as input.keys]
            [gdl.scene2d.actor :refer [visible? set-visible! toggle-visible!]]
            [utils.core :refer [safe-get]]
            [cdq.context :refer [render-map render-entities! tick-entities! line-of-sight? content-grid remove-destroyed-entities! update-mouseover-entity! update-potential-fields! update-elapsed-game-time! debug-render-after-entities debug-render-before-entities set-cursor! transact-all! windows id->window]]
            cdq.context.ui.actors
            [cdq.entity :as entity]
            [cdq.entity.movement :as movement]
            [cdq.state :as state]
            [cdq.world.content-grid :refer [active-entities]]))

; for now a function, see gdl.backends.libgdx.context.input reload bug
; otherwise keys in dev mode may be unbound because dependency order not reflected
; because bind-roots
(defn- hotkey->window []
  {input.keys/i :inventory-window
   input.keys/q :skill-window ; 's' moves also ! (WASD)
   input.keys/e :entity-info-window
   input.keys/h :help-window
   input.keys/z :debug-window})

(defn- check-window-hotkeys [ctx]
  (doseq [[hotkey window] (hotkey->window)
          :when (key-just-pressed? ctx hotkey)]
    (toggle-visible! (id->window ctx window))))

(defn- adjust-zoom [camera by] ; DRY map editor
  (camera/set-zoom! camera (max 0.1 (+ (camera/zoom camera) by))))

(def ^:private zoom-speed 0.05)

(defn- end-of-frame-checks! [{:keys [context/config world-camera] :as context}]
  (when (key-pressed? context input.keys/shift-left)
    (adjust-zoom world-camera  zoom-speed))

  (when (key-pressed? context input.keys/minus)
    (adjust-zoom world-camera (- zoom-speed)))

  (when (safe-get config :debug-windows?)
    (check-window-hotkeys context))

  (when (key-just-pressed? context input.keys/escape)
    (let [windows (windows context)]
      (cond (some visible? windows) (run! #(set-visible! % false) windows)
            :else (change-screen! :screens/options-menu))))

  (when (key-just-pressed? context input.keys/tab)
    (change-screen! :screens/minimap)))

(defn- render-game [{:keys [context/player-entity
                            world-camera]
                     :as context}
                    active-entities*]
  (camera/set-position! world-camera (:entity/position @player-entity))
  (render-map context)
  (render-world-view context
                     (fn [context]
                       (debug-render-before-entities context)
                       (render-entities! context
                                         (->> active-entities*
                                              (filter :entity/z-order)
                                              (filter #(line-of-sight? context @player-entity %))))
                       (debug-render-after-entities context))))

(def ^:private pausing? true)

(defn- assoc-delta-time [ctx]
  (assoc ctx :context/delta-time (min (delta-time ctx)
                                      movement/max-delta-time)))

(defn- step-one-frame? [ctx]
  (key-just-pressed? ctx input.keys/p))

(defn- update-game [{:keys [context/player-entity
                            context/game-paused?
                            cdq.context.ecs/thrown-error]
                     :as ctx}
                    active-entities]
  (let [state-obj (entity/state-obj @player-entity)
        _ (transact-all! ctx (state/manual-tick state-obj @player-entity ctx))
        paused? (reset! game-paused? (or @thrown-error
                                         (and pausing?
                                              (state/pause-game? state-obj)
                                              (not (step-one-frame? ctx)))))
        ctx (assoc-delta-time ctx)]
    (update-mouseover-entity! ctx) ; this do always so can get debug info even when game not running
    (when-not paused?
      (update-elapsed-game-time! ctx)
      (update-potential-fields! ctx active-entities)
      (tick-entities! ctx (map deref active-entities)))
    ;(println "~ update-game, call remove-destroyed-entities! ctx ~")
    (remove-destroyed-entities! ctx) ; do not pause this as for example pickup item, should be destroyed.
    ;(println "~ finished update-game call to r-d-es ~")
    (end-of-frame-checks! ctx)))

; TODO RESET WORLD STATE
; * ecs
; * world

; * 1. :tx/destroy ALL entities
; * 2. replay create entities txs (can only do this too)
; * 3. reset player-entity atom

(defn- replay-frame! []

  ; * count game logic frame #
  ; * save TXS for each frame
  ; * just replay them in same order
  ; * first for replay -> reset the world and change to replay-frame logic

  ; * explored tile corners atom (txs ?) - can keep as is for now.

  )

(defrecord SubScreen []
  Screen
  (show [_ _context])

  (hide [_ ctx]
    (set-cursor! ctx :cursors/default))

  (render [_ {:keys [context/player-entity] :as context}]
    (let [active-entities (active-entities (content-grid context) player-entity)]
      (render-game context (map deref active-entities))
      (update-game context active-entities))))

(defn screen [context]
  {:actors (cdq.context.ui.actors/->ui-actors context)
   :sub-screen (->SubScreen)})
