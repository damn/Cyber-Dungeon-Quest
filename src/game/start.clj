(nsx game.start
  (:require [gdl.backends.lwjgl3 :as lwjgl3]
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

(def tile-size 48)
(set-var-root #'g/world-unit-scale (/ 1 tile-size))

(def screen-width 1440)
(def screen-height 900)

; TODO I want to be able to zoom ingame! (mousewheel, touchpad zoom)
(def scale 1)


; TODO save window  position /resizing on restart
; (app get window & pass to lwjgl/create-app ... easy...
; user-config.edn (sound on/off/window fullcsreen/position/etc.)
(def config
  {:title "Cyber Dungeon Quest"
   :width  screen-width
   :height screen-height
   :full-screen false
   :fps nil}) ; TODO dont limit!

(defn create-game []
  (game/create {:mainmenu mainmenu-screen
                :loading loading-screen
                :ingame ingame-screen
                :minimap minimap-screen
                :options options-screen
                :editor tiledmap-renderer}))

(defn app []
  (lwjgl3/create-app (create-game) config))

; Start dev-loop:
; lein run -m gdl.dev-loop game.start app


; Adjust to changes in gdl
; app/defmanaged
; what else ?
