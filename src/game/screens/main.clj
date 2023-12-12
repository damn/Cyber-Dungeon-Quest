(ns game.screens.main
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.input :as input]
            [gdl.lc :as lc]
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.stage :as stage]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.image :as image]
            [game.screens.load-session :refer (is-loaded-character)]
            [game.player.session-data :refer (current-character-name)])
  (:import com.badlogic.gdx.Gdx
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
  (lc/create [[_ {:keys [skip-main-menu bg-image]}] {:keys [assets batch]}]
    (.bindRoot #'skip-main-menu skip-main-menu)
    (.bindRoot #'bg-image (image/create assets bg-image))
    (.bindRoot #'stage (stage/create gui/viewport batch)) ; TODO remove all .bindRoot
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
  (lc/show [_]
    (.setInputProcessor Gdx/input stage))
  (lc/hide [_]
    (.setInputProcessor Gdx/input nil))
  (lc/render [_ context]
    (app/render-with context
                     :gui
                     (fn [context]
                       (image/draw-centered context
                                            bg-image
                                            [(/ (gui/viewport-width)  2)
                                             (/ (gui/viewport-height) 2)])))
    (.draw stage))
  (lc/tick [_ _state delta]
    (.act stage delta)
    (when (input/is-key-pressed? :ESCAPE) ; no input/
      (.exit Gdx/app))
    (when skip-main-menu
      (try-create-character))))
