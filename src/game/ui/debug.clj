(nsx game.ui.debug)

(defn- debug-infos []
  (str "FPS: " (g/fps)  "\n"
       "World: "(mapv int (world/mouse-position)) "\n"
       "X:" ((world/mouse-position) 0) "\n"
       "Y:" ((world/mouse-position) 1) "\n"
       "GUI: " (gui/mouse-position) "\n"
       ;(game.ui.stage/mouseover-gui?)
       (when-not @game.running/running
         (str "\n~~ PAUSED ~~"))))

#_(when-let [error @thrown-error] ; TODO rest with (/ 1 0)
    (str "\nError! See logs!\n " #_error))

#_(defn- fix-size [cell]
  (doto cell
    (.width  (float 150))
    (.height (float 100))))

; private doesnt work here, inside not top level
(app/on-create
 (def window (ui/window "Debug"))
 (def ^:private label (ui/label ""))
 (def ^:private label-cell (.add window label))
 (.add window (ui/actor
               #(do
                 (.setText label (debug-infos))
                 (.pack window)))))
