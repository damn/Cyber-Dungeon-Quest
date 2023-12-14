(ns game.start
  (:require [x.x :refer [defcomponent]]
            [gdl.app :as app]
            [gdl.lifecycle :as lc]
            [gdl.graphics.freetype :as freetype]
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            game.properties
            game.screens.main
            game.screens.load-session
            game.screens.ingame
            game.screens.minimap
            game.screens.options
            mapgen.tiledmap-renderer
            property-editor.screen)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.graphics.g2d.BitmapFont))

(defcomponent :default-font font
  (lc/dispose [_]
    (.dispose ^BitmapFont font)))

; !!!
; TODO make all context stuff namespaced keyword like :context/properties
; !!! greppable !!!

(defn- create-context [context]
  (game.ui.inventory-window/initialize! context)
  (game.ui.action-bar/initialize!)
  (game.player.status-gui/initialize! context)
  (game.screens.main/initialize! context {:bg-image "ui/moon_background.png" :skip-main-menu false})
  (let [properties (let [file "resources/properties.edn"
                         properties (game.properties/load-edn context file)]
                     (.bindRoot #'game.properties/properties-file file)
                     (.bindRoot #'game.properties/properties properties)
                     (println "loaded properties")
                     properties)]
    {:default-font (freetype/generate (.internal Gdx/files "exocet/films.EXL_____.ttf")
                                      16)
     :context/properties properties
     :game.maps.data nil ; disposes tiled-map.
     ; screen lifecycle modules
     :game.screens.main nil
     :game.screens.load-session nil
     :game.screens.ingame (game.screens.ingame/create-stage
                           (assoc context :context/properties properties))
     :game.screens.minimap nil
     :game.screens.options (game.screens.options/create-stage context)
     :mapgen.tiledmap-renderer (mapgen.tiledmap-renderer/create-stage context)
     :property-editor.screen (property-editor.screen/create-stage context)}))

(def app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
         :height 900
         :full-screen? false
         :fps nil} ; TODO fix is set to 60 @ gdl
   :tile-size 48
   :modules create-context
   :first-screen :game.screens.main})

(defn app []
  (app/start app-config))
