(ns screens.main-menu
  (:require [gdl.context :refer [->stage draw-centered-image render-gui-view create-image]]
            gdl.disposable
            gdl.screen
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context change-screen!]]
            context.ecs
            context.mouseover-entity
            context.world-map
            game.ui.action-bar
            game.ui.inventory-window)
  (:import (com.badlogic.gdx Gdx Input$Keys)
           com.badlogic.gdx.scenes.scene2d.Stage))

(defn- init-context [context]
  (game.ui.inventory-window/rebuild-inventory-widgets!) ; before adding entities ( player gets items )
  (game.ui.action-bar/reset-skills!) ; empties skills -> before adding player

  ; TODO z-order namespaced keywords
  (let [context (merge context
                       (context.ecs/->context :z-orders [:on-ground ; items
                                                         :ground    ; creatures, player
                                                         :flying    ; flying creatures
                                                         :effect])  ; projectiles, nova
                       (context.mouseover-entity/->context-map)
                       {:context/update-entities? (atom true)})]
    (context.world-map/merge->context context)))

(defrecord Screen [^Stage stage bg-image]
  gdl.disposable/Disposable
  (dispose [_]
    (.dispose stage))
  gdl.screen/Screen
  (show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (hide [_ _ctx]
    (.setInputProcessor Gdx/input nil))
  (render [_ {:keys [gui-viewport-width gui-viewport-height] :as context}]
    (render-gui-view context
                     (fn [c]
                       (draw-centered-image c
                                            bg-image
                                            [(/ gui-viewport-width  2)
                                             (/ gui-viewport-height 2)])))
    (.draw stage))
  (tick [_ _state delta]
    (.act stage delta)
    (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
      (.exit Gdx/app))))

(defn screen [context {:keys [bg-image]}]
  (let [table (ui/table :rows [[(ui/text-button "Start game"
                                                #(do
                                                  (swap! current-context init-context)
                                                  (change-screen! :screens/game)))]
                               [(ui/text-button "Map editor"
                                                #(change-screen! :screens/map-editor))]
                               [(ui/text-button "Property editor"
                                                #(change-screen! :screens/property-editor))]
                               [(ui/text-button "Exit" #(.exit Gdx/app))]]
                        :cell-defaults {:pad-bottom 25}
                        :fill-parent? true)
        _ (.center table)
        stage (->stage context [table])
        bg-image (create-image context bg-image)]
    (->Screen stage bg-image)))
