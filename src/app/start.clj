(ns app.start
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [generate-ttf ->stage-screen]]
            (context [properties :as properties]
                     builder
                     potential-fields)
            context.effect.all
            context.entity.all
            context.modifier.all
            (context.ui [action-bar :as action-bar]
                        [inventory-window :as inventory]
                        hp-mana-bars)
            (screens game
                     main-menu
                     map-editor
                     minimap
                     options-menu
                     property-editor)))


; TODO check 'internal' data structure use anywhere (id comp..)
; maybe namespaced keyword pattern '::' ?
; => like ecs
; context/world-map used @ render tiledmap screens/game & get explored tile grid @ minimap

(defn- create-context [context]
  (let [context (merge context
                       (properties/->context context "resources/properties.edn")
                       (inventory/->context)
                       (action-bar/->context))]
    (context.ui.hp-mana-bars/initialize! context)
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
         :fps 60}
   :create-context create-context
   :first-screen :screens/main-menu
   :world-unit-scale (/ tile-size)})

(defn -main []
  (app/start app-config))
