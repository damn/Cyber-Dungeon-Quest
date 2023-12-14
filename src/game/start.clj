(ns game.start
  (:require [x.x :refer [defcomponent]]
            [gdl.app :as app]
            [gdl.lifecycle :as lc]
            [gdl.graphics.freetype :as freetype]
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            game.ui.inventory-window
            game.ui.action-bar
            game.player.status-gui
            game.properties
            game.screens.main
            game.screens.ingame
            game.screens.minimap
            game.screens.options
            mapgen.tiledmap-renderer
            property-editor.screen)
  (:import com.badlogic.gdx.Gdx))

; !!!
; TODO make all context stuff namespaced keyword like :context/properties
; ====>>> but defrecords no namespaced keys ? fuck.
; TODO also components,effects,modifiers,effect-params, etc. everything ....
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
                     properties)]
    {:default-font (freetype/generate (.internal Gdx/files "exocet/films.EXL_____.ttf")
                                      16)
     :context/properties properties
     :game.maps.data (game.maps.data/->Disposable-State)
     :screens/main-menu   (game.screens.main/->Screen)
     :screens/ingame      (game.screens.ingame/screen (assoc context :context/properties properties))
     :screens/minimap     (game.screens.minimap/->Screen)
     :screens/option-menu (game.screens.options/screen context)
     :mapgen.tiledmap-renderer (mapgen.tiledmap-renderer/screen context)
     :property-editor.screen (property-editor.screen/screen context)}))

(def app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
         :height 900
         :full-screen? false
         :fps nil} ; TODO fix is set to 60 @ gdl
   :tile-size 48
   :modules create-context
   :first-screen :screens/main-menu})

(defn app []
  (app/start app-config))
