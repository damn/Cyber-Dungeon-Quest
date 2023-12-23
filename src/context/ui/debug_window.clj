(ns context.ui.debug-window
  (:require [gdl.context :refer [gui-mouse-position world-mouse-position frames-per-second
                                 mouse-on-stage-actor? ->actor]]
            [gdl.scene2d.ui :as ui]))

(defn- debug-infos [{:keys [context/game-paused?
                            context/player-entity
                            context.entity/thrown-error
                            context/elapsed-game-time
                            ] :as c}]
  (let [world-mouse (world-mouse-position c)]
    (str "FPS: " (frames-per-second c)  "\n"
         "World: "(mapv int world-mouse) "\n"
         "X:" (world-mouse 0) "\n"
         "Y:" (world-mouse 1) "\n"
         "GUI: " (gui-mouse-position c) "\n"
         (when @thrown-error
           (str "\nERROR!\n " @thrown-error "\n\n"))
         "game-paused? " @game-paused? "\n"
         "elapsed-game-time " (utils.core/readable-number
                               (/ @elapsed-game-time 1000)) " seconds "
         ;"\nMouseover-Actor:\n"
         #_(when-let [actor (mouse-on-stage-actor? c)]
           (str "TRUE - name:" (.getName actor)
                "id: " (gdl.scene2d.actor/id actor)
                )))))

(defn create [context]
  (let [window (ui/window :title "Debug"
                          :id :debug-window)
        label (ui/label "")]
    (.add window label)
    (.add window (->actor context
                          {:act
                           (fn [context]
                             (ui/set-text label (debug-infos context))
                             (.pack window))}))
    window))
