(ns screens.game
  (:require [gdl.app :refer [change-screen!]]
            [gdl.context :refer [delta-time key-just-pressed? key-pressed?]]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as camera]
            [gdl.screen :refer [Screen]]
            [gdl.input.keys :as input.keys]
            [gdl.scene2d.actor :refer [visible? set-visible! toggle-visible!]]
            [utils.core :refer [safe-get]]
            [cdq.api.context :refer [render-map render-entities! tick-entities! line-of-sight? content-grid remove-destroyed-entities! update-mouseover-entity! update-potential-fields! update-elapsed-game-time! debug-render-after-entities debug-render-before-entities set-cursor! transact-all! frame->txs windows id->window]]
            cdq.context.ui.actors
            [cdq.api.entity :as entity]
            [cdq.entity.movement :as movement]
            [cdq.api.state :as state]
            [cdq.api.world.content-grid :refer [active-entities]]))

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

(defn- end-of-frame-checks! [{:keys [context/config]
                              {:keys [world-camera]} :context/graphics
                              :as context}]
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

(defn- render-game [{:keys [context/player-entity]
                     {:keys [world-camera] :as g} :context/graphics
                     :as context}
                    active-entities*]
  (camera/set-position! world-camera (:entity/position @player-entity))
  (render-map context)
  (g/render-world-view g
                       (fn [g]
                         (debug-render-before-entities context g)
                         (render-entities! context
                                           g
                                           ; TODO lazy seqS everywhere!
                                           (->> active-entities*
                                                (filter :entity/z-order)
                                                (filter #(line-of-sight? context @player-entity %))))
                         (debug-render-after-entities context g))))

(def ^:private pausing? true)

(defn- assoc-delta-time [ctx]
  (assoc ctx :context/delta-time (min (delta-time ctx)
                                      movement/max-delta-time)))

(defn- step-one-frame? [ctx]
  (key-just-pressed? ctx input.keys/p))

(defn- update-game [{:keys [context/player-entity
                            context/game-paused?
                            cdq.context.ecs/thrown-error
                            context/game-logic-frame]
                     :as ctx}
                    active-entities]
  (let [state-obj (entity/state-obj @player-entity)
        _ (transact-all! ctx (state/manual-tick state-obj @player-entity ctx))
        paused? (reset! game-paused? (or @thrown-error
                                         (and pausing?
                                              (state/pause-game? (entity/state-obj @player-entity))
                                              (not (step-one-frame? ctx)))))
        ctx (assoc-delta-time ctx)]
    (update-mouseover-entity! ctx) ; this do always so can get debug info even when game not running
    (when-not paused?
      (swap! game-logic-frame inc)
      (update-elapsed-game-time! ctx)
      (update-potential-fields! ctx active-entities)
      (tick-entities! ctx (map deref active-entities))) ; TODO lazy seqs everywhere!
    (remove-destroyed-entities! ctx) ; do not pause this as for example pickup item, should be destroyed.
    (end-of-frame-checks! ctx)))

(defn- replay-frame! [ctx frame-number]
  (update-mouseover-entity! ctx)
  (update-elapsed-game-time! (assoc-delta-time ctx))
  (let [txs (frame->txs ctx frame-number)]
    ;(println frame-number ". " (count txs))
    (transact-all! ctx txs))
  (end-of-frame-checks! ctx))

; TODO adjust sound speed also equally ? pitch ?
(def ^:private replay-speed 2)

(defn- replay-game! [{:keys [context/game-logic-frame] :as ctx}]
  (dotimes [_ replay-speed]
    (replay-frame! ctx (swap! game-logic-frame inc))))

(defrecord SubScreen []
  Screen
  (show [_ _context])

  (hide [_ ctx]
    (set-cursor! ctx :cursors/default))

  (render [_ {:keys [context/player-entity
                     context/replay-mode?] :as context}]
    (let [active-entities (active-entities (content-grid context) player-entity)]
      ; TODO lazy seqS everywhere!
      (render-game context (map deref active-entities))
      (if replay-mode?
        (replay-game! context)
        (update-game context active-entities)))))

(defn screen [context]
  {:actors (cdq.context.ui.actors/->ui-actors context)
   :sub-screen (->SubScreen)})
