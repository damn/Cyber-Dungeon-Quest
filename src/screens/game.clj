(ns screens.game
  (:require [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.lifecycle :as lc]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui :as ui]
            [utils.core :refer [safe-get]]
            [game.ui.debug-window :as debug-window]
            [game.ui.help-window :as help-window]
            [game.ui.entity-info-window :as entity-info-window]
            [game.ui.skill-window :as skill-window]
            [game.ui.inventory-window :as inventory]
            [game.ui.action-bar :as action-bar]
            game.tick
            game.render)
  (:import com.badlogic.gdx.Gdx
           (com.badlogic.gdx.scenes.scene2d Actor Stage)))

(defn- item-on-cursor-render-actor []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (let [{:keys [drawer gui-mouse-position context/player-entity] :as context} (app/current-context)]
        (when-let [item (:item-on-cursor @player-entity)]
          ; windows keep changing z-index when selected, or put all windows in 1 group and this actor another group
          (.toFront ^Actor this)
          (draw/centered-image drawer
                               (:image item)
                               gui-mouse-position))))))

; MOVE to game/ui ?
(defn- create-stage [{:keys [batch
                             gui-viewport
                             gui-viewport-width
                             gui-viewport-height]
                      :as context}]
  (let [^Actor debug-window (debug-window/create)
        ^Actor help-window (help-window/create)
        ^Actor entity-info-window (entity-info-window/create)
        skill-window (skill-window/create context)
        windows [debug-window
                 help-window
                 entity-info-window
                 inventory/window
                 skill-window]
        stage (stage/create gui-viewport batch)
        table (ui/table :rows [[{:actor action-bar/horizontal-group :expand? true :bottom? true}]]
                        :fill-parent? true)]
    (.addActor stage table)
    (.setPosition debug-window 0 gui-viewport-height)
    (.setPosition help-window
                  (- (/ gui-viewport-width 2)
                     (/ (.getWidth help-window) 2))
                  gui-viewport-height)
    (.setPosition inventory/window
                  gui-viewport-width
                  (- (/ gui-viewport-height 2)
                     (/ (.getHeight help-window) 2)))
    (.setPosition entity-info-window (.getX inventory/window) 0)
    (.setWidth entity-info-window (.getWidth inventory/window))
    (.setHeight entity-info-window (.getY inventory/window))
    (doseq [window windows]
      (.addActor stage window))
    (.addActor stage (item-on-cursor-render-actor))
    stage))

(defrecord IngameScreen [^Stage stage]
  lc/Disposable
  (lc/dispose [_]
    (.dispose stage))
  lc/Screen
  (lc/show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ context]
    (game.render/render-game context)
    (.draw stage))
  (lc/tick [_ context delta]
    (game.tick/tick-game context stage delta)
    (.act stage delta)))

(defn screen [context]
  (->IngameScreen (create-stage context)))
