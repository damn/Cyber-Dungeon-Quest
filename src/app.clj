(ns app
  (:require [x.x :refer [defcomponent]]
            [gdl.app :as app]
            [gdl.lifecycle :as lc]
            [gdl.graphics.freetype :as freetype]
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            game.ui.inventory-window
            game.ui.action-bar
            game.ui.hp-mana-bars
            properties
            screens.game
            screens.main-menu
            screens.map-editor
            screens.minimap
            screens.options-menu
            screens.property-editor)
  (:import com.badlogic.gdx.Gdx))

; !!!
; TODO make all context stuff namespaced keyword like :context/properties
; ====>>> but defrecords no namespaced keys ? fuck.
; TODO also components,effects,modifiers,effect-params, etc. everything ....
; !!! greppable !!!

(defn- create-context [context]
  (game.ui.inventory-window/initialize! context)
  (game.ui.action-bar/initialize!)
  (game.ui.hp-mana-bars/initialize! context)
  (let [properties (let [file "resources/properties.edn"
                         properties (properties/load-edn context file)]
                     (.bindRoot #'properties/properties-file file)
                     (.bindRoot #'properties/properties properties)
                     properties)]
    {:default-font (freetype/generate (.internal Gdx/files "exocet/films.EXL_____.ttf")
                                      16)
     :context/properties properties
     ; TODO here context/world-map
     ; :game.maps.data (game.maps.data/->Disposable-State)
     :screens/game            (screens.game/screen (assoc context :context/properties properties))
     :screens/main-menu       (screens.main-menu/screen context
                                                        {:bg-image "ui/moon_background.png"
                                                         :skip-main-menu false})
     :screens/map-editor      (screens.map-editor/screen context)
     :screens/minimap         (screens.minimap/->Screen)
     :screens/options-menu    (screens.options-menu/screen context)
     :screens/property-editor (screens.property-editor/screen context)}))

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
