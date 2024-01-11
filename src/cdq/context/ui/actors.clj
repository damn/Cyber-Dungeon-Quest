(ns cdq.context.ui.actors
  (:require [gdl.app :refer [change-screen!]]
            [gdl.context :refer [->actor ->table ->group ->text-button get-stage]]
            [gdl.scene2d.actor :refer [toggle-visible! add-tooltip!]]
            [gdl.scene2d.group :refer [children]]
            [cdq.context.ui.hp-mana-bars :refer [->hp-mana-bars]]
            [cdq.context.ui.debug-window :as debug-window]
            [cdq.context.ui.help-window :as help-window]
            [cdq.context.ui.entity-info-window :as entity-info-window]
            [cdq.context.ui.skill-window :as skill-window]
            [cdq.context.ui.inventory-window :as inventory]
            [cdq.context.ui.player-message :refer [->player-message-actor]]
            [cdq.context :refer [->action-bar id->window]]
            [cdq.state.player-item-on-cursor :refer [draw-item-on-cursor]]))

(defn- ->item-on-cursor-actor [context]
  (->actor context {:draw draw-item-on-cursor}))

(extend-type gdl.context.Context
  cdq.context/Windows
  (windows [ctx]
    (children (:windows (get-stage ctx))))

  (id->window [ctx window-id]
    (get (:windows (get-stage ctx)) window-id)))

; TODO reused from properties, move gdl.
(defn ->sprite [ctx sprite-idx]
  (gdl.backends.libgdx.context.image-drawer-creator/map->Image
   (gdl.context/get-sprite ctx
                           {:file "ui/uf_interface.png"
                            :tilew 24
                            :tileh 24}
                           sprite-idx)))

(defn- ->option-button-cell [ctx sprite-idx tooltip-text f]
  {:actor (let [button  (gdl.context/->image-button ctx
                                                    (->sprite ctx sprite-idx)
                                                    f
                                                    {:dimensions [32 32]})]
            (add-tooltip! button (fn [_] tooltip-text))
            button)
   :bottom? true})

; TODO change-screen here ok?
; TODO debug-windows disable when not debug mode
(defn- ->base-table [ctx]
  (->table ctx {:rows [[{:actor (->action-bar ctx)
                         :expand? true
                         :bottom? true
                         :left? true}
                        (->option-button-cell ctx [7 1] "Options" (fn [_] (change-screen! :screens/options-menu)))
                        (->option-button-cell ctx [4 1] "Minimap" (fn [_] (change-screen! :screens/minimap)))
                        (->option-button-cell ctx [10 1] "Inventory" (fn [ctx] (toggle-visible! (id->window ctx :inventory-window))))
                        (->option-button-cell ctx [6 1] "Skills" (fn [ctx] (toggle-visible! (id->window ctx :skill-window))))
                        (->option-button-cell ctx [9 0] "Help" (fn [ctx] (toggle-visible! (id->window ctx :help-window))))
                        (->option-button-cell ctx [8 0] "Entity Info" (fn [ctx] (toggle-visible! (id->window ctx :entity-info-window))))
                        (->option-button-cell ctx [3 0] "Debug" (fn [ctx] (toggle-visible! (id->window ctx :debug-window))))]]
                :cell-defaults {:pad 2}
                :fill-parent? true}))

(defn- ->windows [context]
  (->group context {:id :windows
                    :actors [(debug-window/create context)
                             (help-window/create context)
                             (entity-info-window/create context)
                             (inventory/->inventory-window context)
                             (skill-window/create context)]}))

(defn ->ui-actors [ctx]
  [(->base-table           ctx)
   (->hp-mana-bars         ctx)
   (->windows              ctx)
   (->item-on-cursor-actor ctx)
   (->player-message-actor ctx)])
