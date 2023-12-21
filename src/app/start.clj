(ns app.start
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [generate-ttf ->stage-screen]]
            (context [properties :as properties]
                     builder
                     potential-fields)
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            (game.ui inventory-window
                     action-bar
                     hp-mana-bars)
            (screens game
                     main-menu
                     map-editor
                     minimap
                     options-menu
                     property-editor)))

(defn- create-context [context]
  (let [context (merge context
                       (properties/->context context "resources/properties.edn"))]
    (game.ui.inventory-window/initialize! context)
    (game.ui.action-bar/initialize!)
    (game.ui.hp-mana-bars/initialize! context)
    (merge context
           ; previous default-font overwritten
           {:default-font (generate-ttf context {:file "exocet/films.EXL_____.ttf"
                                                 :size 16})
            :screens/game            (->stage-screen context (screens.game/screen context))
            :screens/main-menu       (->stage-screen context (screens.main-menu/screen context {:bg-image "ui/moon_background.png"}))
            :screens/map-editor      (->stage-screen context (screens.map-editor/screen context))
            :screens/minimap         (screens.minimap/->Screen)
            :screens/options-menu    (->stage-screen context (screens.options-menu/screen context))
            :screens/property-editor (->stage-screen context (screens.property-editor/screen context))})))

(def ^:private tile-size 48)

(def ^:private app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440
         :height 900
         :full-screen? false
         :fps 300}
   :create-context create-context
   :first-screen :screens/main-menu
   :world-unit-scale (/ tile-size)})

(defn app []
  (app/start app-config))
