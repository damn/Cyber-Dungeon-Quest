(ns game.ui.debug-window
  (:require [gdl.graphics :as g]
            [gdl.graphics.world :as world]
            [gdl.graphics.gui :as gui]
            [gdl.ui :as ui]
            game.running))

(defn- debug-infos []
  (str "FPS: " (g/fps)  "\n"
       "World: "(mapv int (world/mouse-position)) "\n"
       "X:" ((world/mouse-position) 0) "\n"
       "Y:" ((world/mouse-position) 1) "\n"
       "GUI: " (gui/mouse-position) "\n"
       ;(game.ui.stage/mouseover-gui?)
       (when-not @game.running/running
         (str "\n~~ PAUSED ~~"))))

#_(when-let [error @thrown-error] ; TODO test with (/ 1 0)
    (str "\nError! See logs!\n " #_error))

(defn create []
  (let [window (ui/window :title "Debug")
        label (ui/label "")]
    (.add window label)
    (.add window (ui/actor
                  #(do
                    (.setText label (debug-infos))
                    (.pack window))))
    window))
