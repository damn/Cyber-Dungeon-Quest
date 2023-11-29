(ns game.start
  (:require [gdl.app :as app]
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            game.screens.main
            game.screens.load-session
            game.screens.ingame
            game.screens.minimap
            game.screens.options
            mapgen.tiledmap-renderer
            property-editor.screen))

(def app-config
  {:window {:title "Cyber Dungeon Quest"
            :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
            :height 900
            :full-screen false
            :fps nil} ; TODO fix is set to 60 @ gdl
   :tile-size 48
   :log-lc? false
   :modules [[:game.properties "resources/properties.edn"]
             [:game.maps.data]
             [:game.media]
             [:game.ui.inventory-window]
             [:game.ui.action-bar]
             [:game.player.status-gui]
             [:game.screens.main {:bg-image "ui/moon_background.png"
                                  :skip-main-menu false}]
             [:game.screens.load-session]
             [:game.screens.ingame]
             [:game.screens.minimap]
             [:game.screens.options]
             [:mapgen.tiledmap-renderer]
             [:property-editor.screen]]
   :first-screen :game.screens.main})

(defn app []
  (app/start app-config))
