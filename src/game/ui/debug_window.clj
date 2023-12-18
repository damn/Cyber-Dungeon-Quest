(ns game.ui.debug-window
  (:require [gdl.app :as app]
            [gdl.scene2d.ui :as ui])
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.scenes.scene2d.Actor))

(defn- debug-infos [{:keys [gui-mouse-position
                            world-mouse-position
                            context/running
                            context/player-entity
                            context/thrown-error]}]
  (str "FPS: " (.getFramesPerSecond Gdx/graphics)  "\n"
       "World: "(mapv int world-mouse-position) "\n"
       "X:" (world-mouse-position 0) "\n"
       "Y:" (world-mouse-position 1) "\n"
       "GUI: " gui-mouse-position "\n"
       (when @thrown-error
         (str "\nERROR!\n " @thrown-error "\n\n"))
       "Game running? " @running "\n"))

(defn create []
  ; TODO just a self-updating window with textfn and title/id
  ; textfn-window
  (let [window (ui/window :title "Debug"
                          :id :debug-window)
        label (ui/label "")]
    (.add window label)
    (.add window (proxy [Actor] []
                   (act [_delta]
                     (let [context (app/current-context)]
                       (ui/set-text label (debug-infos context))
                       (.pack window)))))
    window))
