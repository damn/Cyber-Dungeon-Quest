(nsx game.ui.debug)

(defn- debug-infos []
  (str "FPS: " (g/fps)  "\n"
       "World: "(mapv int (g/map-coords)) "\n"
       "X:" ((g/map-coords) 0) "\n"
       "Y:" ((g/map-coords) 1) "\n"
       "GUI: " (g/mouse-coords) "\n"
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
