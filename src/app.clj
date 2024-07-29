(ns app
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [generate-ttf]]
            cdq.properties
            (cdq.context [properties :as properties]
                         [cursor :as cursor]
                         builder
                         effect
                         modifier
                         potential-fields
                         render-debug
                         [screens :as screens]
                         transaction-handler)
            (cdq.context.ui [action-bar :as action-bar]
                            [inventory-window :as inventory-window]
                            player-modal
                            error-modal)
            [cdq.api.context :refer [set-cursor!]]))

(def ^:private production-config
  {:map-editor? false
   :property-editor? false
   :debug-windows? false
   :debug-options? false})

(def ^:private dev-config
  {:map-editor? true
   :property-editor? true
   :debug-windows? true
   :debug-options? true})

(def ^:private config dev-config)

(defn- create-context [default-context]
  (let [context (merge default-context
                       {:context/properties (properties/->context default-context
                                                                  {:file "resources/properties.edn"
                                                                   :property-types cdq.properties/property-types})})
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
           {:context/screens (screens/->context context)})))

(def ^:private app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440
         :height 900
         :full-screen? false
         :fps 60}
   :create-context create-context
   :world-unit-scale (/ 48)})

(defn -main []
  (app/start app-config))
