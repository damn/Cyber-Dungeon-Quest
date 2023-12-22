(ns game.tick
  (:require [gdl.context :refer [get-stage key-just-pressed?]]
            [gdl.input.keys :as input.keys]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui :refer [find-actor-with-id]]
            [app.state :refer [change-screen!]]
            [game.context :refer [tick-entity remove-destroyed-entities update-mouseover-entity update-potential-fields]]
            [game.components.movement :as movement]
            [game.entity :as entity]
            [game.ui.action-bar :as action-bar])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Group)))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

(def ^:private pausing true)

(def ^:private hotkey->window
  {input.keys/i :inventory-window
   input.keys/q :skill-window ; 's' moves also ! (WASD)
   input.keys/e :entity-info-window
   input.keys/h :help-window
   input.keys/z :debug-window})

(defn- check-window-hotkeys [context group]
  (doseq [[hotkey window] hotkey->window
          :when (key-just-pressed? context hotkey)]
    (actor/toggle-visible! (find-actor-with-id group window))))

; TODO rename check-change-screen
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
             (= :dead (entity/get-state @player-entity)))
    (change-screen! :screens/main-menu)))

(defn tick-game [{:keys [context/player-entity
                         context/update-entities?
                         context/thrown-error]
                  :as context}
                 active-entities
                 delta]
  (action-bar/up-skill-hotkeys)
  (let [state (:state-obj (:entity/state @player-entity))
        _ (entity/manual-tick! state context delta)
        pause-game? (or @thrown-error
                        (and pausing (entity/pause-game? state)))
        update? (reset! update-entities? (not pause-game?))
        delta (limit-delta delta)]
    ; this do always so can get debug info even when game not running
    (update-mouseover-entity context)
    (when update?
      ; sowieso keine bewegungen / kein update gemacht ? checkt nur tiles ?
      (update-potential-fields context active-entities)
      (doseq [entity active-entities]
        (tick-entity context entity delta))))
  ; do not pause this as for example pickup item, should be destroyed.
  (remove-destroyed-entities context)
  (end-of-frame-checks context))
