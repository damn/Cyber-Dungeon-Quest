(nsx game.screens.main
  (:require [game.screens.load-session :refer (is-loaded-character)]
            [game.player.session-data :refer (current-character-name)])
  (:import com.badlogic.gdx.scenes.scene2d.Stage))

(defn- start-loading-game [character-name & {new-character :new-character}]
  (reset! is-loaded-character (not new-character))
  (reset! current-character-name character-name)
  (gdl.app/set-screen :loading))

(defn- try-create-character []
  (when-let [char-name "FOO BAR"]
    (start-loading-game (apply str char-name) :new-character true)))

(app/defmanaged ^:dispose ^Stage stage (ui/stage))

(app/on-create
 (let [table (ui/table)]
   (.setFillParent table true)
   (.addActor stage table)
   (.center table)
   (.setDebug table false)
   (let [new-game-button (ui/text-button "New game" try-create-character)
         editor-button   (ui/text-button "Editor" #(gdl.app/set-screen :editor))
         exit-button     (ui/text-button "Exit" gdl.app/exit)
         padding 25]
     (.padBottom (.add table new-game-button) (float padding))
     (.row table)
     (.padBottom (.add table editor-button)   (float padding))
     (.row table)
     (.add table exit-button)))

 (def menu-bg-image (image/create "ui/moon_background.png")))

(defn- render* []
  (image/draw-centered menu-bg-image
                       [(/ (gui/viewport-width)  2)
                        (/ (gui/viewport-height) 2)])
  (ui/draw-stage stage))

(def ^:private skip-main-menu true)

(def mainmenu-screen
  (reify gdl.app/Screen
    (show [_]
      (input/set-processor stage))
    (render [_]
      (gui/render render*))
    (tick [_ delta]
      (ui/update-stage stage delta)
      (when (input/is-key-pressed? :ESCAPE)
        (gdl.app/exit))
      (when skip-main-menu
        (try-create-character)))))
