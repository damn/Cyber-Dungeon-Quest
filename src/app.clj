(ns app
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [generate-ttf ->stage-screen ->image-widget create-image]]
            [utils.core :refer [safe-get]]
            (cdq.context [properties :as properties]
                         [cursor :as cursor]
                         builder
                         effect
                         modifier
                         potential-fields
                         render-debug
                         transaction-handler)
            [cdq.entity.movement :refer [frames-per-second]]
            (cdq.context.ui [action-bar :as action-bar]
                            [inventory-window :as inventory-window]
                            player-modal
                            error-modal)
            (screens game
                     main-menu
                     map-editor
                     minimap
                     options-menu
                     property-editor)
            [cdq.api.context :refer [set-cursor!]]))

(def ^:private production-config
  {:full-screen? true
   :map-editor? false
   :property-editor? false
   :debug-windows? false
   :debug-options? false
   :assert? false})

(def ^:private dev-config
  {:full-screen? false
   :map-editor? true
   :property-editor? true
   :debug-windows? true
   :debug-options? true
   :assert? true})

(def ^:private config dev-config)

; TODO do @ gloal vars, has to be done before ns requires.
; (set! *assert* (safe-get config :assert?))

(defn- screens [ctx]
  (let [->background-image #(->image-widget ctx ; fn because cannot add actor to different stages
                                            (create-image ctx "ui/moon_background.png")
                                            {:fill-parent? true
                                             :scaling :fill
                                             :align :center})]
    {:screens/game            (->stage-screen ctx (screens.game/screen ctx))
     :screens/main-menu       (->stage-screen ctx (screens.main-menu/screen ctx (->background-image)))
     :screens/map-editor      (->stage-screen ctx (screens.map-editor/screen ctx))
     :screens/minimap         (screens.minimap/->Screen)
     :screens/options-menu    (->stage-screen ctx (screens.options-menu/screen ctx (->background-image)))
     :screens/property-editor (->stage-screen ctx (screens.property-editor/screen ctx (->background-image)))}))

(defn- create-context [default-context]
  (let [context (merge default-context
                       (properties/->context default-context "resources/properties.edn"))
        context (merge context
                       (cursor/->context context)
                       (inventory-window/->context context)
                       (action-bar/->context context)
                       {:context/config config})
        context (assoc-in context
                          [:context/graphics :default-font]
                          (generate-ttf context {:file "exocet/films.EXL_____.ttf" :size 16}))]
    (set-cursor! context :cursors/default)
    (merge context
           {:context/screens (screens context)})))

(def ^:private tile-size 48)

(def ^:private app-config
  {:app {:title "Vampire Queen"
         :width  1440
         :height 900
         :full-screen? (safe-get config :full-screen?)
         :fps frames-per-second}
   :create-context create-context
   :first-screen :screens/main-menu
   :world-unit-scale (/ tile-size)})

(defn -main []
  (app/start app-config))
