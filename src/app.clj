(ns app
  (:require [gdl.app :as app]
            [gdl.protocols :refer [generate-ttf]]
            gdl.default-context
            [game.protocols :refer [create-gui-stage]]
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            game.ui.actors
            game.ui.inventory-window
            game.ui.action-bar
            game.ui.hp-mana-bars
            properties
            game.tick
            game.render
            game.render.debug
            screens.game
            screens.main-menu
            screens.map-editor
            screens.minimap
            screens.options-menu
            screens.property-editor)
  (:import com.badlogic.gdx.Gdx))

(defn- create-context []
  (let [context (gdl.default-context/->Context)
        properties (let [file "resources/properties.edn"
                         properties (properties/load-edn context file)]
                     (.bindRoot #'properties/properties-file file)
                     (.bindRoot #'properties/properties properties)
                     properties)
        context (merge context {:context/properties properties})]
    (game.ui.inventory-window/initialize! context)
    (game.ui.action-bar/initialize!)
    (game.ui.hp-mana-bars/initialize! context)
    (merge context
           {:default-font (generate-ttf context {:file "exocet/films.EXL_____.ttf"
                                                 :size 16})

            ; TODO here context/world-map
            ; :game.maps.data (game.maps.data/->Disposable-State)
            :screens/game            (-> context
                                         (create-gui-stage (game.ui.actors/create-actors context))
                                         screens.game/->Screen)
            :screens/main-menu       (screens.main-menu/screen context {:bg-image "ui/moon_background.png"
                                                                        :skip-main-menu false})
            :screens/map-editor      (screens.map-editor/screen context)
            :screens/minimap         (screens.minimap/->Screen)
            :screens/options-menu    (screens.options-menu/screen context)
            :screens/property-editor (screens.property-editor/screen context)})))

(def app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
         :height 900
         :full-screen? false
         :fps nil} ; TODO fix is set to 60 @ gdl
   :tile-size 48
   :context-fn create-context
   :first-screen :screens/main-menu})

(defn app []
  (app/start app-config))
