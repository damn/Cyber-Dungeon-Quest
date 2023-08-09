(nsx game.start
  (:require [gdl.game :as game]
            [mapgen.tiledmap-renderer :refer (tiledmap-renderer)]
            (game.screens [main         :refer (mainmenu-screen)]
                          [load-session :refer  (loading-screen)]
                          [ingame       :refer   (ingame-screen)]
                          [minimap      :refer  (minimap-screen)]
                          [options      :refer  (options-screen)])

            (game.components image
                             animation
                             delete-after-animation-stopped?
                             ; TODO forgot but refresh takes it! dangerous !
                             ; how to check ? (autoload all in folder & subfolder ?
                             ; possible such a command ??
                             ; => components in a game defined in a config as data? active/not-active ???
                             )
            [game.components.body.rotation-angle]
            )
  ;(:gen-class)
  )


; The most frequently used resolutions on laptop and 2-in-1 PCs nowadays are
; 1366-by-768 (also known as HD) and
; 1920-by-1080 (Full HD or 1080p).
; 1920-by-1080 is the most appropriate screen resolution for laptops, in our opinion.

; MBP
; 1440 & 900  ; TODO adjust resolution ! black bars ! 16-10 , hd is 17.8 - 10 (extendviewport / limited by player light radius range -> bigger screens possible ?)
; 24x24 sci-fi tileset
; 60x37.5 tiles on screen

; scaled x2 for better readability ( test)
; 30x18.75 tiles on screen
; -> viewport 720 x 450
; screen size 1440 & 900 (or set full screen)

; 57x32 24x24 px modules
; 16 modules
; 228 x 128 tiles for the area-transitions.
; 114 x 64
; 16x9

; 28.5 tiles -> 32 width module size -> 128
; 16 -> 20 height modules size - 80

; TODO save window  position /resizing on restart
; (app get window & pass to lwjgl/create-app ... easy...
; user-config.edn (sound on/off/window fullcsreen/position/etc.)
(def window-config
  {:title "Cyber Dungeon Quest"
   :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
   :height 900
   :full-screen false
   :fps nil}) ; TODO dont limit! / config missign ?

(defn app []
  (game/start {:screens {:mainmenu mainmenu-screen
                         :loading loading-screen
                         :ingame ingame-screen
                         :minimap minimap-screen
                         :options options-screen
                         :editor tiledmap-renderer}
               :tile-size 48
               :window window-config
               :log-lc? true
               :ns-components [[:x.temp]]}))
