(ns game.tick
  (:require [gdl.context :refer [gui-mouse-position]]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [app.state :refer [change-screen!]]
            [game.context :refer [tick-active-entities
                                    destroy-to-be-removed-entities!
                                    update-mouseover-entity]]
            [game.components.movement :as movement]
            [game.components.state :as state]
            [game.ui.action-bar :as action-bar]
            [game.maps.potential-field :refer [update-potential-fields]])
  (:import (com.badlogic.gdx Gdx Input$Keys Input$Buttons)
           com.badlogic.gdx.scenes.scene2d.Actor))

(defn- update-game-systems
  [{:keys [context/game-running?] :as context} stage delta]
  ; TODO stage part of context
  ; destroy here not @ tick, because when game is paused
  ; for example pickup item, should be destroyed. TODO fix - weird ! I want to do just context/tick ...
  ; => finally the whole context just swap , but we like atoms?
  ; => should use a removelist and after tick immediately remove, not next frame ....
  (destroy-to-be-removed-entities! context)

  ; this do always so can get debug info even when game not running
  ; TODO move stage in context , can do stage/hit inside update
  (update-mouseover-entity context (stage/hit stage (gui-mouse-position context)))

  (when @game-running? ; sowieso keine bewegungen / kein update gemacht ? checkt nur tiles ?
    (update-potential-fields context)))

(defn- limit-delta [delta]
  (min delta movement/max-delta))

(def ^:private pausing true)

(defn- check-window-hotkeys [{:keys [debug-window
                                 inventory-window
                                 entity-info-window
                                 skill-window
                                 help-window] :as stage}]
  (let [windows [debug-window
                 help-window
                 entity-info-window
                 inventory-window
                 skill-window]])
  ; TODO entity/skill info also
  (when (.isKeyJustPressed Gdx/input Input$Keys/I)
    (actor/toggle-visible! inventory-window)
    (actor/toggle-visible! entity-info-window)
    (actor/toggle-visible! skill-window))
  (when (.isKeyJustPressed Gdx/input Input$Keys/H)
    (actor/toggle-visible! help-window))
  (when (.isKeyJustPressed Gdx/input Input$Keys/Z)
    (actor/toggle-visible! debug-window)))

(defn- end-of-frame-checks [{:keys [context/player-entity]}]
  (when (.isKeyJustPressed Gdx/input Input$Keys/TAB)
    (change-screen! :screens/minimap))
  (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
    (cond
     (some #(.isVisible ^Actor %) windows)
     (run! #(.setVisible ^Actor % false) windows)
     :else
     (change-screen! :screens/options-menu)))
  (when (and (.isKeyJustPressed Gdx/input Input$Keys/X)
             (= :dead (:state (:fsm (:components/state @player-entity)))))
    (change-screen! :screens/main-menu)))

(extend-type gdl.context.Context
  game.context/GameScreenTick
  (tick-game [{:keys [context/player-entity
                      context/game-running?
                      context/thrown-error]
               ; TODO call ecs/thrown-error ?? move the whole ECS component in 1 map itself ? easier overview
               ; when browsing context
               :as context}
              stage
              delta]
    (action-bar/up-skill-hotkeys)
    (check-window-hotkeys stage)
    (let [state (:state-obj (:components/state @player-entity))]
      (state/manual-tick! state context delta)
      (let [pause-game? (or @thrown-error
                            (and pausing (state/pause-game? state)))]
        (reset! game-running? (not pause-game?))))
    (let [delta (limit-delta delta)]
      (update-game-systems context stage delta)
      (when @game-running?
        (tick-active-entities context delta)))
    (end-of-frame-checks context)))

