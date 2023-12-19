(ns game.tick
  (:require [gdl.app :as app]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [game.protocols :refer [tick-active-entities
                                    destroy-to-be-removed-entities!
                                    update-mouseover-entity]]
            [game.components.movement :as movement]
            [game.components.state :as state]
            [game.ui.action-bar :as action-bar]
            [game.maps.potential-field :refer [update-potential-fields]])
  (:import (com.badlogic.gdx Gdx Input$Keys Input$Buttons)
           com.badlogic.gdx.scenes.scene2d.Actor))

(defn- update-game-systems [{:keys [gui-mouse-position
                                    context/running]
                             :as context}
                            stage ; TODO stage part of context
                            delta]
  ; destroy here not @ tick, because when game is paused
  ; for example pickup item, should be destroyed. TODO fix - weird ! I want to do just context/tick ...
  ; => finally the whole context just swap , but we like atoms?
  ; => should use a removelist and after tick immediately remove, not next frame ....
  (destroy-to-be-removed-entities! context)

  ; this do always so can get debug info even when game not running
  (update-mouseover-entity context (stage/hit stage gui-mouse-position))

  (when @running ; sowieso keine bewegungen / kein update gemacht ? checkt nur tiles ?
    (update-potential-fields context)))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

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


(extend-type gdl.context.Context
  game.protocols/GameScreenTick
  (tick-game [{:keys [context/player-entity
                      context/running
                      context/thrown-error]
               ; TODO call ecs/thrown-error ?? move the whole ECS component in 1 map itself ? easier overview
               ; when browsing app/state
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
    (let [delta (limit-delta delta)]
      (update-game-systems context stage delta)
      (when @running
        (tick-active-entities context delta)))))

