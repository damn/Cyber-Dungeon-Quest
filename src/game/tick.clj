(ns game.tick
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [update-map doseq-entity]]
            [gdl.app :as app]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [game.context :as gm]
            [game.entity :as entity]
            [game.components.movement :as movement]
            [game.components.state :as state]
            [game.ui.action-bar :as action-bar]
            [game.context.mouseover-entity :refer (update-mouseover-entity)]
            [game.maps.contentfields :refer [get-entities-in-active-content-fields]]
            [game.maps.potential-field :refer [update-potential-fields]])
  (:import (com.badlogic.gdx Gdx Input$Keys Input$Buttons)
           com.badlogic.gdx.scenes.scene2d.Actor))

(defn- tick-entity! [context entity delta]
  (swap! entity update-map entity/tick delta)
  ; (doseq-entity entity entity/tick! context delta)
  (doseq [k (keys @entity)] ; TODO FIXME
    (entity/tick! [k (k @entity)] context entity delta)))

(defn- update-game-systems [{:keys [gui-mouse-position
                                    context/running]
                             :as context}
                            stage
                            delta]
  ; destroy here not @ tick, because when game is paused
  ; for example pickup item, should be destroyed.
  (gm/destroy-to-be-removed-entities! context)
  (update-mouseover-entity context (stage/hit stage gui-mouse-position))
  (when @running ; sowieso keine bewegungen / kein update gemacht ? checkt nur tiles ?
    (update-potential-fields context)))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

(def thrown-error (atom nil)) ; TODO global state ! => REMOVE ALL GLOBAL STATE NOW

(def ^:private pausing true)

(defn- handle-key-input [{:keys [debug-window
                                 inventory-window
                                 entity-info-window
                                 skill-window
                                 help-window] :as stage}
                         {:keys [gui-mouse-position
                                 world-mouse-position
                                 context/player-entity
                                 context/mouseover-entity]
                          :as context}]
  (action-bar/up-skill-hotkeys)
  (let [windows [debug-window
                 help-window
                 entity-info-window
                 inventory-window
                 skill-window]]
    (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
      (cond
       (some #(.isVisible ^Actor %) windows)
       (run! #(.setVisible ^Actor % false) windows)
       :else
       (app/change-screen! :screens/options-menu))))

  (when (.isKeyJustPressed Gdx/input Input$Keys/TAB)
    (app/change-screen! :screens/minimap)) ; TODO does  do it immediately (cancel the current frame ) or finish this frame?
  ; TODO entity/skill info also
  (when (.isKeyJustPressed Gdx/input Input$Keys/I)
    (actor/toggle-visible! inventory-window)
    (actor/toggle-visible! entity-info-window)
    (actor/toggle-visible! skill-window))
  (when (.isKeyJustPressed Gdx/input Input$Keys/H)
    (actor/toggle-visible! help-window))
  (when (.isKeyJustPressed Gdx/input Input$Keys/Z)
    (actor/toggle-visible! debug-window)))

(defn tick-game [{:keys [context/player-entity
                         context/running]
                  :as context}
                 stage
                 delta]
  (handle-key-input stage context)
  (let [state (:state-obj (:components/state @player-entity))]
    (state/manual-tick! state context delta)
    (reset! running (if (or @thrown-error
                            (and pausing (state/pause-game? state)))
                      false
                      true)))
  (try (let [delta (limit-delta delta)]
         (update-game-systems context stage delta)
         (when @running
           (doseq [entity (get-entities-in-active-content-fields context)]
             (tick-entity! context entity delta))))
       (catch Throwable t
         (println "Catched throwable: ")
         (p/pretty-pst t) ;=> we already have this @ gdl ?
         (reset! thrown-error t))))
