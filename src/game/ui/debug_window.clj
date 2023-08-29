(ns game.ui.debug-window
  (:require [gdl.graphics :as g]
            [gdl.graphics.world :as world]
            [gdl.graphics.gui :as gui]
            [gdl.scene2d.ui :as ui]
            game.running))

(defn- debug-infos []
  (str "FPS: " (g/fps)  "\n"
       "World: "(mapv int (world/mouse-position)) "\n"
       "X:" ((world/mouse-position) 0) "\n"
       "Y:" ((world/mouse-position) 1) "\n"
       "GUI: " (gui/mouse-position) "\n"
       (when-not @game.running/running
         (str "\n~~ PAUSED ~~"))))

(defn create []
  (let [window (ui/window :title "Debug"
                          :id :debug-window)
        label (ui/label "")]
    (.add window label)
    (.add window (ui/actor
                  #(do
                    (.setText label (debug-infos))
                    (.pack window))))
    window))
