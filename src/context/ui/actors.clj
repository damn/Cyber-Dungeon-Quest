(ns context.ui.actors
  (:require [gdl.app :refer [current-context]]
            [gdl.context :refer [draw-centered-image gui-mouse-position draw-text]]
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.actor :as actor]
            [game.context :refer [->player-message-actor
                                  ->action-bar]]
            [game.entity :as entity]
            [context.ui.hp-mana-bars :refer [->hp-mana-bars]]
            [context.ui.debug-window :as debug-window]
            [context.ui.help-window :as help-window]
            [context.ui.entity-info-window :as entity-info-window]
            [context.ui.skill-window :as skill-window]
            [context.ui.inventory-window :as inventory])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Group)))

(defn- draw-item-on-cursor [{:keys [context/player-entity] :as c}]
  (let [{:keys [context/player-entity] :as context} @current-context]
    (when (= :item-on-cursor (entity/state @player-entity))
      (draw-centered-image context
                           (:image (:item-on-cursor @player-entity))
                           (gui-mouse-position context)))))

(defn- ->item-on-cursor-actor []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (draw-item-on-cursor @current-context))))

(defn- ->base-table [context]
  (ui/table :rows [[{:actor (->action-bar context)
                     :expand? true
                     :bottom? true}]]
            :fill-parent? true))

(defn- ->windows [{:keys [gui-viewport-width
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
        inventory-window (inventory/->inventory-window context)
        group (Group.)]
    (actor/set-id group :windows)
    (.setPosition inventory-window
                  gui-viewport-width
                  (- (/ gui-viewport-height 2)
                     (/ (.getHeight help-window) 2)))
    (.setPosition entity-info-window (.getX inventory-window) 0)
    (.setWidth entity-info-window (.getWidth inventory-window))
    (.setHeight entity-info-window (.getY inventory-window))
    (.addActor group debug-window)
    (.addActor group help-window)
    (.addActor group entity-info-window)
    (.addActor group inventory-window)
    (.addActor group (skill-window/create context))
    group))

(defn ->ui-actors [context]
  [(->base-table context)
   (->hp-mana-bars context)
   (->windows context)
   (->item-on-cursor-actor)
   (->player-message-actor context)])
