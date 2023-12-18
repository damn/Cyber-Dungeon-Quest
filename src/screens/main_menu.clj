(ns screens.main-menu
  (:require [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.lifecycle :as lc]
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.stage :as stage]
            [gdl.graphics.image :as image]
            game.session)
  (:import (com.badlogic.gdx Gdx Input$Keys)
           com.badlogic.gdx.scenes.scene2d.Stage))

(declare ^Stage stage
         ^:private skip-main-menu
         ^:private bg-image)

(defn- initialize! [{:keys [gui-viewport batch] :as context}
                    {:keys [skip-main-menu bg-image]}]
  (.bindRoot #'skip-main-menu skip-main-menu)
  (.bindRoot #'bg-image (image/create context bg-image))
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
  lc/Disposable
  (lc/dispose [_]
    (.dispose stage))
  lc/Screen
  (lc/show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_ _ctx]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ {:keys [gui-viewport-width gui-viewport-height] :as context}]
    (app/render-view context
                     :gui
                     (fn [drawer]
                       (draw/centered-image drawer
                                            bg-image
                                            [(/ gui-viewport-width  2)
                                             (/ gui-viewport-height 2)])))
    (.draw stage))
  (lc/tick [_ _state delta]
    (.act stage delta)
    (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
      (.exit Gdx/app))
    #_(when skip-main-menu
      (start-session))))

(defn screen [context config]
  (initialize! context config)
  (->Screen))
