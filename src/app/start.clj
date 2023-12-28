(ns app.start
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [generate-ttf ->stage-screen]]
            [utils.core :refer [safe-get]]
            (context [properties :as properties]
                     [cursor :as cursor]
                     builder
                     potential-fields
                     render-debug
                     skill-info)
            context.effect.all
            context.entity.all
            context.modifier.all
            (context.ui [action-bar :as action-bar]
                        [inventory-window :as inventory-window]
                        player-modal)
            (screens game
                     main-menu
                     map-editor
                     minimap
                     options-menu
                     property-editor)
            [cdq.context :refer [set-cursor!]]))

(def ^:private production-config
  {:full-screen? true
   :map-editor? false
   :property-editor? false
   :debug-windows? false
   :debug-options? false})

(def ^:private dev-config
  {:full-screen? false
   :map-editor? true
   :property-editor? true
   :debug-windows? true
   :debug-options? true})

; TODO move to resources & pass cmdline arg
(def ^:private config dev-config)

(defn- create-context [context]
  (let [context (merge context
                       (properties/->context context "resources/properties.edn"))
        context (merge context
                       (cursor/->context context)
                       (inventory-window/->context context)
                       (action-bar/->context context)
                       {:context/config config})]
    (set-cursor! context :cursors/default)
    (merge context
           ; previous default-font overwritten
           {:default-font (generate-ttf context {:file "exocet/films.EXL_____.ttf" :size 16})
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
         :full-screen? (safe-get config :full-screen?)
         :fps 60}
   :create-context create-context
   :first-screen :screens/main-menu
   :world-unit-scale (/ tile-size)})

(defn -main []
  (app/start app-config))
