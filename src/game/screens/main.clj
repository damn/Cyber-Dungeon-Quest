(ns game.screens.main
  (:require [x.x :refer [defmodule]]
            [gdl.app :as app]
            [gdl.utils :refer [dispose]]
            [gdl.input :as input]
            [gdl.lc :as lc]
            [gdl.scene2d.ui :as ui]
            [gdl.scene2d.stage :as stage]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.image :as image]
            [game.context :as context]
            [game.screens.load-session :refer (is-loaded-character)]
            [game.player.session-data :refer (current-character-name)]))

; TODO do all loading in 'loading' ns...

(defn- start-loading-game [character-name & {new-character :new-character}]
  (reset! is-loaded-character (not new-character))
  (reset! current-character-name character-name)
  (app/set-screen :game.screens.load-session))

(defn- try-create-character []
  (when-let [char-name "FOO BAR"]
    (start-loading-game (apply str char-name) :new-character true)))

(declare stage
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
                                 [(ui/text-button "Exit" app/exit)]]
                          :cell-defaults {:pad-bottom 25}
                          :fill-parent? true)]
      (stage/add-actor stage table)
      (.center table)))
  (lc/dispose [_] (dispose stage))
  (lc/show [_] (input/set-processor stage))
  (lc/hide [_] (input/set-processor nil))
  (lc/render [_ {:keys [batch]}]
    (gui/render batch
                (fn [unit-scale]
                  (image/draw-centered (context/get-context unit-scale)
                                       bg-image
                                       [(/ (gui/viewport-width)  2)
                                        (/ (gui/viewport-height) 2)])
                  (stage/draw stage batch))))
  (lc/tick [_ _state delta]
    (stage/act stage delta)
    (when (input/is-key-pressed? :ESCAPE) ; no input/
      (app/exit))
    (when skip-main-menu
      (try-create-character))))
