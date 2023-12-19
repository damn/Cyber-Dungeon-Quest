(ns game.ui.actors
  (:require [gdl.context :refer [draw-centered-image gui-mouse-position]]
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context]]
            [game.ui.debug-window :as debug-window]
            [game.ui.help-window :as help-window]
            [game.ui.entity-info-window :as entity-info-window]
            [game.ui.skill-window :as skill-window]
            [game.ui.inventory-window :as inventory]
            [game.ui.action-bar :as action-bar])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- item-on-cursor-render-actor []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (let [{:keys [context/player-entity] :as c} @current-context]
        (when (= :item-on-cursor
                 (:state (:fsm (:components/state @player-entity))))
          ; windows keep changing z-index when selected
          (.toFront ^Actor this)
          (draw-centered-image c
                               (:image (:item-on-cursor @player-entity))
                               (gui-mouse-position c)))))))

(defn create-actors [{:keys [gui-viewport-width
                             gui-viewport-height]
                      :as context}]
  (let [^Actor debug-window (debug-window/create)
        _ (.setPosition debug-window 0 gui-viewport-height)

        ^Actor help-window (help-window/create)
        _ (.setPosition help-window
                        (- (/ gui-viewport-width 2)
                           (/ (.getWidth help-window) 2))
                        gui-viewport-height)

        ^Actor entity-info-window (entity-info-window/create)
        skill-window (skill-window/create context)]
    (.setPosition inventory/window
                  gui-viewport-width
                  (- (/ gui-viewport-height 2)
                     (/ (.getHeight help-window) 2)))
    (.setPosition entity-info-window (.getX inventory/window) 0)
    (.setWidth entity-info-window (.getWidth inventory/window))
    (.setHeight entity-info-window (.getY inventory/window))
    [debug-window
     help-window
     entity-info-window
     inventory/window
     skill-window
     (ui/table :rows [[{:actor action-bar/horizontal-group :expand? true :bottom? true}]]
               :fill-parent? true)
     (item-on-cursor-render-actor)]))
