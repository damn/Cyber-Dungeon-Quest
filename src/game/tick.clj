(ns game.tick
  (:require [gdl.context :refer [get-stage key-just-pressed?]]
            [gdl.input.keys :as input.keys]
            [gdl.scene2d.actor :as actor]
            [app.state :refer [change-screen!]]
            [game.context :refer [tick-active-entities destroy-to-be-removed-entities! update-mouseover-entity
                                  update-potential-fields]]
            [game.components.movement :as movement]
            [game.entity :as entity]

            ; => context
            [game.ui.action-bar :as action-bar])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- update-context-systems
  [{:keys [context/update-entities?] :as context} delta]
  ; destroy here not @ tick, because when game is paused
  ; for example pickup item, should be destroyed. TODO fix - weird ! I want to do just context/tick ...
  ; => finally the whole context just swap , but we like atoms?
  ; => should use a removelist and after tick immediately remove, not next frame ....
  (destroy-to-be-removed-entities! context)

  ; this do always so can get debug info even when game not running
  (update-mouseover-entity context)

  (when @update-entities? ; sowieso keine bewegungen / kein update gemacht ? checkt nur tiles ?
    (update-potential-fields context)))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

(def ^:private pausing true)

(defn- end-of-frame-checks [{:keys [context/player-entity] :as context}]
  (let [{:keys [debug-window
                inventory-window
                entity-info-window
                skill-window
                help-window] :as stage} (get-stage context)
        windows [debug-window
                 help-window
                 entity-info-window
                 inventory-window
                 skill-window]]
    (when (key-just-pressed? context input.keys/i)
      (actor/toggle-visible! inventory-window)
      (actor/toggle-visible! entity-info-window)
      (actor/toggle-visible! skill-window))
    (when (key-just-pressed? context input.keys/h)
      (actor/toggle-visible! help-window))
    (when (key-just-pressed? context input.keys/z)
      (actor/toggle-visible! debug-window))
    (when (key-just-pressed? context input.keys/escape)
      (cond
       (some #(.isVisible ^Actor %) windows)
       (run! #(.setVisible ^Actor % false) windows)
       :else
       (change-screen! :screens/options-menu))))
  (when (key-just-pressed? context input.keys/tab)
    (change-screen! :screens/minimap))
  (when (and (key-just-pressed? context input.keys/x)
             (= :dead (:state (:fsm (:entity/state @player-entity)))))
    (change-screen! :screens/main-menu)))

(defn tick-game [{:keys [context/player-entity
                         context/update-entities?
                         context/thrown-error]
                  :as context}
                 delta]
  (action-bar/up-skill-hotkeys)
  (let [state (:state-obj (:entity/state @player-entity))
        _ (entity/manual-tick! state context delta)
        pause-game? (or @thrown-error
                        (and pausing (entity/pause-game? state)))
        update? (reset! update-entities? (not pause-game?))
        delta (limit-delta delta)]
    (update-context-systems context delta)
    (when update?
      (tick-active-entities context delta)))
  (end-of-frame-checks context))
