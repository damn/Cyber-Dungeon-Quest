(ns game.ui.actors
  (:require [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.scene2d.ui :as ui]
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
      (let [{:keys [drawer gui-mouse-position context/player-entity] :as context} (app/current-context)]
        (when-let [item (:item-on-cursor @player-entity)]
          ; windows keep changing z-index when selected, or put all windows in 1 group and this actor another group
          (.toFront ^Actor this)
          (draw/centered-image drawer
                               (:image item)
                               gui-mouse-position))))))

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
