(nsx game.screens.main
  (:require [game.screens.load-session :refer (is-loaded-character)]
            [game.player.session-data :refer (current-character-name)]))

(defn- start-loading-game [character-name & {new-character :new-character}]
  (reset! is-loaded-character (not new-character))
  (reset! current-character-name character-name)
  (game/set-screen :loading))

(defn- try-create-character []
  (when-let [char-name "FOO BAR"]
    (start-loading-game (apply str char-name) :new-character true)))

(ui/def-stage stage)

(app/on-create
 (let [table (ui/table)]
   (.setFillParent table true)
   (.addActor stage table)
   (.center table)
   (.setDebug table false)
   (let [new-game-button (ui/text-button "New game" try-create-character)
         editor-button   (ui/text-button "Editor" #(game/set-screen :editor))
         exit-button     (ui/text-button "Exit" app/exit)
         padding 25]
     (.padBottom (.add table new-game-button) (float padding))
     (.row table)
     (.padBottom (.add table editor-button)   (float padding))
     (.row table)
     (.add table exit-button)))

 (def menu-bg-image (image/create "ui/moon_background.png")))

(defn- render* []
  (image/draw-centered menu-bg-image
                       [(/ (g/viewport-width)  2)
                        (/ (g/viewport-height) 2)])
  (ui/draw-stage stage))

(def ^:private skip-main-menu true)

(game/defscreen mainmenu-screen
  :show (fn []
          (input/set-processor stage))
  :dispose (fn [])
  :render (fn []
            (g/render-gui render*))
  :update (fn [delta]
            (ui/update-stage stage delta)
            (when (input/is-key-pressed? :ESCAPE)
              (app/exit))
            (when skip-main-menu
              (try-create-character))))
