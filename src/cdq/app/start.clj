(ns cdq.app.start
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [generate-ttf ->stage-screen ->image-widget create-image]]
            [utils.core :refer [safe-get]]
            (cdq.context [properties :as properties]
                         [cursor :as cursor]
                         builder
                         effect
                         potential-fields
                         render-debug)
            cdq.entity.all
            [cdq.entity.movement :refer [frames-per-second]]
            (cdq.context.ui [action-bar :as action-bar]
                            [inventory-window :as inventory-window]
                            player-modal
                            error-modal)
            (cdq.screens game
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

(defn- create-context [default-context]
  (println "CREAT ECONTEXT ")
  (let [context (merge default-context
                       (properties/->context default-context "resources/properties.edn"))
        context (merge context
                       (cursor/->context context)
                       (inventory-window/->context context)
                       (action-bar/->context context)
                       {:context/config config})
        ->background-image #(->image-widget context ; fn because cannot add actor to different stages
                                            (create-image context "ui/moon_background.png")
                                            {:fill-parent? true
                                             :scaling :fill
                                             :align :center})]
    (set-cursor! context :cursors/default)
    (merge context
           ; previous default-font overwritten
           {:default-font (generate-ttf context {:file "exocet/films.EXL_____.ttf" :size 16})
            :screens/game            (->stage-screen context (cdq.screens.game/screen context))
            :screens/main-menu       (->stage-screen context (cdq.screens.main-menu/screen context (->background-image)))
            :screens/map-editor      (->stage-screen context (cdq.screens.map-editor/screen context))
            :screens/minimap         (cdq.screens.minimap/->Screen)
            :screens/options-menu    (->stage-screen context (cdq.screens.options-menu/screen context (->background-image)))
            :screens/property-editor (->stage-screen context (cdq.screens.property-editor/screen context (->background-image)))})))

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
