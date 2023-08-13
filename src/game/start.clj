(ns game.start
  (:require gdl.game
            ; require
            game.components.image
            game.components.animation
            game.components.delete-after-animation-stopped?
            game.components.body.rotation-angle))

; TODO save window  position /resizing on restart
; (app get window & pass to lwjgl/create-app ... easy...
; user-config.edn (sound on/off/window fullcsreen/position/etc.)
(def window
  {:title "Cyber Dungeon Quest"
   :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
   :height 900
   :full-screen false
   :fps nil}) ; TODO dont limit! / config missign ?



(comment

 ; remove 'screens'
 ; just pass :first-screen :game.screens.main
 ; game/app hold 'state' of all components
 ; and passes system to current-screen which is just a keyword
 ; => all application state in 1 atom
 ; => use applicationlistener interface?
 ; => 'show' we can trigger ourself on show-screen

 ; => the whole application config is just edn data !

 )

(def modules
  [[:x.temp]
   [:game.screens.main {:bg-image "ui/moon_background.png"
                        :skip-main-menu false}]
   [:game.screens.load-session]
   [:game.screens.ingame]
   [:game.screens.minimap]
   [:game.screens.options]
   [:mapgen.tiledmap-renderer]])

(defn app []
  (gdl.app/start {:window window
                  :tile-size 48
                  :log-lc? true
                  :modules modules
                  :first-screen :game.screens.main}))
