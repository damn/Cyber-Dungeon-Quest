(ns screens.game
  (:require [gdl.app :refer [current-context change-screen!]]
            [gdl.context :refer [get-stage render-world-view delta-time draw-text key-just-pressed?]]
            [gdl.screen :refer [Screen]]
            [gdl.maps.tiled :as tiled]
            [gdl.graphics.color :as color]
            [gdl.graphics.camera :as camera]
            [gdl.input.keys :as input.keys]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui :refer [find-actor-with-id]]
            [cdq.context :refer [render-entities* ray-blocked? explored? set-explored! line-of-sight? content-grid
                                  tick-entity remove-destroyed-entities update-mouseover-entity update-potential-fields
                                  update-elapsed-game-time debug-render-after-entities debug-render-before-entities]]
            [cdq.entity :as entity]
            [context.entity.movement :as movement]
            [context.entity.state :as state]
            context.ui.actors
            [cdq.world.content-grid :refer [active-entities]])
  (:import com.badlogic.gdx.graphics.Color
           (com.badlogic.gdx.scenes.scene2d Actor Group)))

(def ^:private explored-tile-color (Color. (float 0.5)
                                           (float 0.5)
                                           (float 0.5)
                                           (float 1)))

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
      base-color
      (do
       (when-not explored?
         (set-explored! context position))
       color/white))))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

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
    (actor/toggle-visible! (find-actor-with-id group window))))

(defn- end-of-frame-checks [{:keys [context/player-entity] :as context}]
  (let [group (:windows (get-stage context))
        windows (seq (.getChildren ^Group group))]
    (check-window-hotkeys context group)

    (when (key-just-pressed? context input.keys/escape)
      (cond (some #(.isVisible ^Actor %)        windows)
            (run! #(.setVisible ^Actor % false) windows)
            :else
            (change-screen! :screens/options-menu))))

  (when (key-just-pressed? context input.keys/tab)
    (change-screen! :screens/minimap))

  (when (and (key-just-pressed? context input.keys/x)
             (= :dead (entity/state @player-entity)))
    (change-screen! :screens/main-menu)))

(defn- render-game [{:keys [context/world-map
                            context/player-entity
                            world-camera]
                     :as context}
                    active-entities]
  (camera/set-position! world-camera (:position @player-entity))
  (tiled/render-map context
                    (:tiled-map world-map)
                    tile-color-setter)
  (render-world-view context
                     (fn [context]
                       (debug-render-before-entities context)
                       (render-entities* context
                                         (->> active-entities
                                              (map deref)
                                              (filter #(line-of-sight? context @player-entity %))))
                       (debug-render-after-entities context))))

(def ^:private pausing true)

(defn- update-game [{:keys [context/player-entity
                            context/game-paused?
                            context.entity/thrown-error]
                     :as context}
                    active-entities
                    delta]
  (let [state (:state-obj (:entity/state @player-entity)) ; ? Entity protocol?
        _ (state/manual-tick! state context delta)
        paused? (reset! game-paused? (or @thrown-error
                                         (and pausing (state/pause-game? state))))
        delta (limit-delta delta)]
    ; this do always so can get debug info even when game not running
    (update-mouseover-entity context)
    (when-not paused?
      (update-elapsed-game-time context delta)
      ; sowieso keine bewegungen / kein update gemacht ? checkt nur tiles ?
      (update-potential-fields context active-entities)
      (doseq [entity active-entities]
        (tick-entity context entity delta))))
  ; do not pause this as for example pickup item, should be destroyed.
  (remove-destroyed-entities context)
  (end-of-frame-checks context))

(defrecord SubScreen []
  Screen
  (show [_ _context])
  (hide [_ _context])
  (render [_ {:keys [context/player-entity] :as context}]
    (let [active-entities (active-entities (content-grid context) player-entity)
          delta (* (delta-time context) 1000)] ; TODO make in seconds ? no need to multiply by 1000 ?
      (render-game context active-entities)
      (update-game context active-entities delta))))

(defn screen [context]
  {:actors (context.ui.actors/->ui-actors context)
   :sub-screen (screens.game/->SubScreen)})
