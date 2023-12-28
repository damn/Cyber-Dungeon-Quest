(ns context.ui.actors
  (:require [gdl.context :refer [draw-text ->actor ->table ->group]]
            [gdl.scene2d.actor :as actor :refer [set-position! get-x get-y width height set-width! set-height! set-visible!]]
            [gdl.scene2d.group :refer [add-actor!]]
            [context.ui.hp-mana-bars :refer [->hp-mana-bars]]
            [context.ui.debug-window :as debug-window]
            [context.ui.help-window :as help-window]
            [context.ui.entity-info-window :as entity-info-window]
            [context.ui.skill-window :as skill-window]
            [context.ui.inventory-window :as inventory]
            [context.entity.state.player-item-on-cursor :refer [draw-item-on-cursor]]
            [cdq.context :refer [->player-message-actor ->action-bar]]))

(defn- ->item-on-cursor-actor [context]
  (->actor context {:draw draw-item-on-cursor}))

(defn- ->base-table [context]
  (->table context {:rows [[{:actor (->action-bar context) :expand? true :bottom? true}]]
                    :fill-parent? true}))

(defn- ->windows [{:keys [gui-viewport-width
                          gui-viewport-height
                          context/config]
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
        group (->group context)
        skill-window (skill-window/create context)]
    (actor/set-id! group :windows)
    (set-position! inventory-window
                  gui-viewport-width
                  (- (/ gui-viewport-height 2)
                     (/ (height help-window) 2)))
    (set-position! entity-info-window (get-x inventory-window) 0)
    (set-width! entity-info-window (width inventory-window))
    (set-height! entity-info-window (get-y inventory-window))

    (set-visible! debug-window false)
    (set-visible! help-window false)
    (set-visible! entity-info-window false)
    (set-visible! inventory-window false)
    (set-visible! skill-window false)

    (add-actor! group debug-window)
    (add-actor! group help-window)
    (add-actor! group entity-info-window)
    (add-actor! group inventory-window)
    (add-actor! group skill-window)
    group))

(defn ->ui-actors [ctx]
  [(->base-table           ctx)
   (->hp-mana-bars         ctx)
   (->windows              ctx)
   (->item-on-cursor-actor ctx)
   (->player-message-actor ctx)])
