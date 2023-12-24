(ns context.ui.actors
  (:require [gdl.context :refer [draw-centered-image gui-mouse-position draw-text ->actor ->table
                                 ->group]]
            [gdl.scene2d.actor :as actor :refer [set-position! get-x get-y width height set-width! set-height!]]
            [gdl.scene2d.group :refer [add-actor!]]
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
  (->table context {:rows [[{:actor (->action-bar context) :expand? true :bottom? true}]]
                    :fill-parent? true}))

(defn- ->windows [{:keys [gui-viewport-width
                          gui-viewport-height]
                   :as context}]
  (let [debug-window (debug-window/create context)
        _ (set-position! debug-window 0 gui-viewport-height)
        help-window (help-window/create context)
        _ (set-position! help-window
                        (- (/ gui-viewport-width 2)
                           (/ (width help-window) 2))
                        gui-viewport-height)
        entity-info-window (entity-info-window/create context)
        inventory-window (inventory/->inventory-window context)
        group (->group context)]
    (actor/set-id! group :windows)
    (set-position! inventory-window
                  gui-viewport-width
                  (- (/ gui-viewport-height 2)
                     (/ (height help-window) 2)))
    (set-position! entity-info-window (get-x inventory-window) 0)
    (set-width! entity-info-window (width inventory-window))
    (set-height! entity-info-window (get-y inventory-window))
    (add-actor! group debug-window)
    (add-actor! group help-window)
    (add-actor! group entity-info-window)
    (add-actor! group inventory-window)
    (add-actor! group (skill-window/create context))
    group))

(defn ->ui-actors [context]
  [(->base-table           context)
   (->hp-mana-bars         context)
   (->windows              context)
   (->item-on-cursor-actor context)
   (->player-message-actor context)])
