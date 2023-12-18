(ns screens.main-menu
  (:require [gdl.app :as app]
            [gdl.protocols :refer [draw-centered-image render-gui-view create-image]]
            gdl.screen
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.stage :as stage]
            game.session)
  (:import (com.badlogic.gdx Gdx Input$Keys)
           com.badlogic.gdx.scenes.scene2d.Stage))

(declare ^Stage stage
         ^:private skip-main-menu
         ^:private bg-image)

(defn- initialize! [{:keys [gui-viewport batch] :as context}
                    {:keys [skip-main-menu bg-image]}]
  (.bindRoot #'skip-main-menu skip-main-menu)
  (.bindRoot #'bg-image (create-image context bg-image))
  (.bindRoot #'stage (stage/create gui-viewport batch))
  (let [table (ui/table :rows [[(ui/text-button "Start game"
                                                #(do
                                                  (swap! app/state game.session/init-context)
                                                  (app/change-screen! :screens/game)))]
                               [(ui/text-button "Map editor"
                                                #(app/change-screen! :screens/map-editor))]
                               [(ui/text-button "Property editor"
                                                #(app/change-screen! :screens/property-editor))]
                               [(ui/text-button "Exit" #(.exit Gdx/app))]]
                        :cell-defaults {:pad-bottom 25}
                        :fill-parent? true)]
    (.addActor stage table)
    (.center table)))

(defrecord Screen []
  gdl.protocols/Disposable
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
      (.exit Gdx/app))
    #_(when skip-main-menu
      (start-session))))

(defn screen [context config]
  (initialize! context config)
  (->Screen))
