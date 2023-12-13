(ns game.screens.main
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.draw :as draw]
            [gdl.lifecycle :as lc]
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.stage :as stage]
            [gdl.graphics.image :as image]
            [game.screens.load-session :refer (is-loaded-character)]
            [game.player.session-data :refer (current-character-name)])
  (:import (com.badlogic.gdx Gdx Input$Keys)
           com.badlogic.gdx.scenes.scene2d.Stage))

; TODO do all loading in 'loading' ns...

(defn- start-loading-game [character-name & {new-character :new-character}]
  (reset! is-loaded-character (not new-character))
  (reset! current-character-name character-name)
  (app/set-screen :game.screens.load-session))

(defn- try-create-character []
  (when-let [char-name "FOO BAR"]
    (start-loading-game (apply str char-name) :new-character true)))

(declare ^Stage stage
         ^:private skip-main-menu
         ^:private bg-image)

(defmodule _
  (lc/create [[_ {:keys [skip-main-menu bg-image]}] {:keys [gui-viewport batch] :as context}]
    (.bindRoot #'skip-main-menu skip-main-menu)
    (.bindRoot #'bg-image (image/create context bg-image))
    (.bindRoot #'stage (stage/create gui-viewport batch)) ; TODO remove all .bindRoot
    (let [table (ui/table :rows [[(ui/text-button "New game" try-create-character)]
                                 [(ui/text-button "Map Editor" #(app/set-screen :mapgen.tiledmap-renderer))]
                                 [(ui/text-button "Entity Editor" #(app/set-screen :property-editor.screen))]
                                 [(ui/text-button "Exit" #(.exit Gdx/app))]]
                          :cell-defaults {:pad-bottom 25}
                          :fill-parent? true)]
      (.addActor stage table)
      (.center table)))
  (lc/dispose [_]
    (.dispose stage))
  (lc/show [_ _ctx]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ {:keys [gui-viewport-width gui-viewport-height] :as context}]
    (app/render-with context
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
    (when skip-main-menu
      (try-create-character))))
