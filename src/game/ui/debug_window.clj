(ns game.ui.debug-window
  (:require [gdl.context :refer [gui-mouse-position world-mouse-position frames-per-second
                                 mouse-on-stage-actor?]]
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context]])
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- debug-infos [{:keys [context/game-paused?
                            context/player-entity
                            context.ecs/thrown-error] :as c}]
  (let [world-mouse (world-mouse-position c)]
    (str "FPS: " (frames-per-second c)  "\n"
         "World: "(mapv int world-mouse) "\n"
         "X:" (world-mouse 0) "\n"
         "Y:" (world-mouse 1) "\n"
         "GUI: " (gui-mouse-position c) "\n"
         (when @thrown-error
           (str "\nERROR!\n " @thrown-error "\n\n"))
         "game-paused? " @game-paused? "\n"
         ;"\nMouseover-Actor:\n"
         #_(when-let [actor (mouse-on-stage-actor? c)]
           (str "TRUE - name:" (.getName actor)
                "id: " (gdl.scene2d.actor/id actor)
                )))))

(defn create []
  (let [window (ui/window :title "Debug"
                          :id :debug-window)
        label (ui/label "")]
    (.add window label)
    (.add window (proxy [Actor] []
                   (act [_delta]
                     (ui/set-text label (debug-infos @current-context))
                     (.pack window))))
    window))
