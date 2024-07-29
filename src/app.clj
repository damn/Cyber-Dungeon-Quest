(ns app
  (:require [core.component :as component]
            [gdl.context :as ctx]
            [gdl.backends.libgdx.app :as app]
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
                            error-modal)))

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

(component/def :context/config {}
  tag
  (ctx/create [_ _ctx] config))

(def ^:private app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440
         :height 900
         :full-screen? false
         :fps 60}
   :context [[:context/graphics {:tile-size 48
                                 :default-font {:file "exocet/films.EXL_____.ttf" :size 16}}]
             [:context/assets true]
             [:context/ui true]
             [:context/properties {:file "resources/properties.edn"}]
             [:context/cursors true]
             [:context/inventory true]
             [:cdq.context.ui.action-bar/data true]
             [:context/config config]
             ; requires context/config (debug-windows)
             ; make asserts .... for all dependencies ... everywhere o.o
             [:context/screens true]]})

(defn -main []
  (app/start app-config))
