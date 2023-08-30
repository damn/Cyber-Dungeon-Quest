(ns game.screens.main
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.input :as input]
            [gdl.lc :as lc]
            [gdl.scene2d.ui :as ui]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.image :as image]
            [gdl.graphics.batch :refer [batch]]
            [game.screens.load-session :refer (is-loaded-character)]
            [game.player.session-data :refer (current-character-name)])
  (:import com.badlogic.gdx.scenes.scene2d.Stage))

; TODO do all loading in 'loading' ns...

(defn- start-loading-game [character-name & {new-character :new-character}]
  (reset! is-loaded-character (not new-character))
  (reset! current-character-name character-name)
  (app/set-screen :game.screens.load-session))

(defn- try-create-character []
  (when-let [char-name "FOO BAR"]
    (start-loading-game (apply str char-name) :new-character true)))

(declare ^Stage stage)

(defn- create* []
  (.bindRoot #'stage (ui/stage gui/viewport batch)) ; TODO remove all .bindRoot
  (let [table (ui/table :rows [[(ui/text-button "New game" try-create-character)]
                               [(ui/text-button "Map Editor" #(app/set-screen :mapgen.tiledmap-renderer))]
                               [(ui/text-button "Entity Editor" #(app/set-screen :entity-editor.screen))]
                               [(ui/text-button "Exit" app/exit)]]
                        :cell-defaults {:pad-bottom 25}
                        :fill-parent? true)]
    (.addActor stage table)
    (.center table)))

(declare ^:private skip-main-menu
         ^:private bg-image)

(defmodule _
  (lc/create [[_ {:keys [skip-main-menu bg-image]}]]
    (.bindRoot #'skip-main-menu skip-main-menu)
    (.bindRoot #'bg-image (image/create bg-image))
    (create*))
  (lc/dispose [_]
    (.dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_]
    (gui/render
     (fn []
       (image/draw-centered bg-image
                            [(/ (gui/viewport-width)  2)
                             (/ (gui/viewport-height) 2)])
       (ui/draw-stage stage batch))))
  (lc/tick [_ delta]
    (ui/update-stage stage delta) ; act
    (when (input/is-key-pressed? :ESCAPE) ; no input/
      (app/exit))
    (when skip-main-menu
      (try-create-character))))
