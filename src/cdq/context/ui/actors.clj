(ns cdq.context.ui.actors
  (:require [gdl.context :refer [->actor ->table ->group]]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.group :refer [add-actor!]]
            [cdq.context.ui.hp-mana-bars :refer [->hp-mana-bars]]
            [cdq.context.ui.debug-window :as debug-window]
            [cdq.context.ui.help-window :as help-window]
            [cdq.context.ui.entity-info-window :as entity-info-window]
            [cdq.context.ui.skill-window :as skill-window]
            [cdq.context.ui.inventory-window :as inventory]
            [cdq.context.entity.state.player-item-on-cursor :refer [draw-item-on-cursor]]
            [cdq.context :refer [->player-message-actor ->action-bar]]))

(defn- ->item-on-cursor-actor [context]
  (->actor context {:draw draw-item-on-cursor}))

(defn- ->base-table [context]
  (->table context {:rows [[{:actor (->action-bar context) :expand? true :bottom? true}]]
                    :fill-parent? true}))

(defn- ->windows [context]
  (let [group (->group context)]
    (actor/set-id! group :windows)
    (add-actor! group (debug-window/create context))
    (add-actor! group (help-window/create context))
    (add-actor! group (entity-info-window/create context))
    (add-actor! group (inventory/->inventory-window context))
    (add-actor! group (skill-window/create context))
    group))

(defn ->ui-actors [ctx]
  [(->base-table           ctx)
   (->hp-mana-bars         ctx)
   (->windows              ctx)
   (->item-on-cursor-actor ctx)
   (->player-message-actor ctx)])
