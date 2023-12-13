(ns game.start
  (:require [x.x :refer [defcomponent]]
            [gdl.app :as app]
            [gdl.lc :as lc]
            [gdl.graphics.freetype :as freetype]
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            game.screens.main
            game.screens.load-session
            game.screens.ingame
            game.screens.minimap
            game.screens.options
            mapgen.tiledmap-renderer
            property-editor.screen)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.graphics.g2d.BitmapFont))

(defcomponent :default-font {:keys [file size]}
  (lc/create [_ context]
    (freetype/generate (.internal Gdx/files file) size))
  (lc/dispose [[_ font]]
    (.dispose ^BitmapFont font)))

(def app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
         :height 900
         :full-screen? false
         :fps nil} ; TODO fix is set to 60 @ gdl
   :tile-size 48
   :modules {:default-font {:file "exocet/films.EXL_____.ttf"
                            :size 16}
             :game.properties "resources/properties.edn"
             :game.maps.data nil
             :game.ui.inventory-window nil
             :game.ui.action-bar nil
             :game.player.status-gui nil
             :game.screens.main {:bg-image "ui/moon_background.png"
                                 :skip-main-menu false}
             :game.screens.load-session nil
             :game.screens.ingame nil
             :game.screens.minimap nil
             :game.screens.options nil
             :mapgen.tiledmap-renderer nil
             :property-editor.screen nil}
   :first-screen :game.screens.main})

(defn app []
  (app/start app-config))
