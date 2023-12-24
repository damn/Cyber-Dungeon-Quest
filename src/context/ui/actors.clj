(ns context.ui.actors
  (:require [gdl.context :refer [draw-centered-image gui-mouse-position draw-text ->actor ->table
                                 ->group]]
            [gdl.scene2d.actor :as actor]
            [cdq.context :refer [->player-message-actor ->action-bar]]
            [cdq.entity :as entity]
            [context.ui.hp-mana-bars :refer [->hp-mana-bars]]
            [context.ui.debug-window :as debug-window]
            [context.ui.help-window :as help-window]
            [context.ui.entity-info-window :as entity-info-window]
            [context.ui.skill-window :as skill-window]
            [context.ui.inventory-window :as inventory]))

(defn- draw-item-on-cursor [{:keys [context/player-entity] :as context}]
  (when (= :item-on-cursor (entity/state @player-entity))
    (draw-centered-image context
                         (:image (:item-on-cursor @player-entity))
                         (gui-mouse-position context))))

(defn- ->item-on-cursor-actor [context]
  (->actor context {:draw draw-item-on-cursor}))

(defn- ->base-table [context]
  (->table context
           {:rows [[{:actor (->action-bar context) :expand? true :bottom? true}]]
            :fill-parent? true}))

(defn- ->windows [{:keys [gui-viewport-width
                          gui-viewport-height]
                   :as context}]
  (let [debug-window (debug-window/create context)
        _ (.setPosition debug-window 0 gui-viewport-height)
        help-window (help-window/create context)
        _ (.setPosition help-window
                        (- (/ gui-viewport-width 2)
                           (/ (.getWidth help-window) 2))
                        gui-viewport-height)
        entity-info-window (entity-info-window/create context)
        inventory-window (inventory/->inventory-window context)
        group (->group context)]
    (actor/set-id! group :windows)
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
  [(->base-table           context)
   (->hp-mana-bars         context)
   (->windows              context)
   (->item-on-cursor-actor context)
   (->player-message-actor context)])
