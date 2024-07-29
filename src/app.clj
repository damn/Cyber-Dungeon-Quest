(ns app
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [generate-ttf]]
            [utils.core :refer [safe-get]]
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
            [cdq.entity.movement :refer [frames-per-second]]
            (cdq.context.ui [action-bar :as action-bar]
                            [inventory-window :as inventory-window]
                            player-modal
                            error-modal)
            [cdq.api.context :refer [set-cursor!]]))

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

(def ^:private tile-size 48)

(def ^:private app-config
  {:app {:title "Vampire Queen"
         :width  1440
         :height 900
         :full-screen? (safe-get config :full-screen?)
         :fps frames-per-second}
   :create-context create-context
   :world-unit-scale (/ tile-size)})

(defn -main []
  (app/start app-config))
