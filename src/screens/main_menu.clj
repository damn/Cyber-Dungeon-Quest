(ns screens.main-menu
  (:require [gdl.context :refer [exit-app draw-centered-image render-gui-view create-image ->text-button key-just-pressed?]]
            [gdl.input.keys :as input.keys]
            gdl.screen
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context change-screen!]]
            context.ecs
            context.mouseover-entity
            [context.ui.player-message :as player-message]
            [context.world :as world]
            [game.context :refer [rebuild-inventory-widgets]]
            context.ui.action-bar))

(defn- init-context [context]
  (rebuild-inventory-widgets context) ; before adding entities ( player gets items )
  (context.ui.action-bar/reset-skills!) ; empties skills -> before adding player

  ; TODO z-order namespaced keywords
  (let [context (merge context
                       (context.ecs/->context :z-orders [:on-ground ; items
                                                         :ground    ; creatures, player
                                                         :flying    ; flying creatures
                                                         :effect])  ; projectiles, nova
                       (context.mouseover-entity/->context)
                       (player-message/->context)
                       {:context/game-paused? (atom true)})]
    (world/merge->context context)))

(defrecord SubScreen [bg-image]
  gdl.screen/Screen
  (show [_ _ctx])
  (hide [_ _ctx])
  (render [_ {:keys [gui-viewport-width gui-viewport-height] :as context}]
    (render-gui-view context
                     (fn [c]
                       (draw-centered-image c
                                            bg-image
                                            [(/ gui-viewport-width  2)
                                             (/ gui-viewport-height 2)])))
    (when (key-just-pressed? context input.keys/escape)
      (exit-app context))))

(defn screen [context {:keys [bg-image]}]
  (let [table (ui/table :rows [[(->text-button context "Start game"
                                               (fn [_context]
                                                 (swap! current-context init-context)
                                                 (change-screen! :screens/game)))]
                               [(->text-button context "Map editor"
                                               (fn [_context]
                                                 (change-screen! :screens/map-editor)))]
                               [(->text-button context "Property editor"
                                               (fn [_context]
                                                 (change-screen! :screens/property-editor)))]
                               [(->text-button context "Exit" exit-app)]]
                        :cell-defaults {:pad-bottom 25}
                        :fill-parent? true)]
    (.center table)
    {:actors [table]
     :sub-screen (->SubScreen (create-image context bg-image))}))
