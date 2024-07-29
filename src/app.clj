(ns app
  (:require [gdl.backends.libgdx.app :as app]
            (cdq.context properties
                         cursor
                         builder
                         config
                         effect
                         modifier
                         potential-fields
                         render-debug
                         screens
                         transaction-handler
                         action-bar
                         inventory-window)
            (cdq.context.ui player-modal
                            error-modal)))

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
             [:cdq.context.action-bar/data true]
             [:context/config {:tag :dev
                               :configs {:prod {:map-editor? false
                                                :property-editor? false
                                                :debug-windows? false
                                                :debug-options? false}
                                         :dev {:map-editor? true
                                               :property-editor? true
                                               :debug-windows? true
                                               :debug-options? true}}}]
             ; requires context/config (debug-windows)
             ; make asserts .... for all dependencies ... everywhere o.o
             [:context/screens true]]})

(defn -main []
  (app/start app-config))
