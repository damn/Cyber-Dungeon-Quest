(ns cdq.context.screens
  (:require [gdl.context :refer [->stage-screen ->image-widget create-image]]
            (cdq.screens game
                         main-menu
                         map-editor
                         minimap
                         options-menu
                         property-editor)))

(defn ->context [ctx]
  (let [->background-image #(->image-widget ctx ; fn because cannot add actor to different stages
                                            (create-image ctx "ui/moon_background.png")
                                            {:fill-parent? true
                                             :scaling :fill
                                             :align :center})]
    {:first-screen :screens/main-menu
     :screens/game            (->stage-screen ctx (cdq.screens.game/screen ctx))
     :screens/main-menu       (->stage-screen ctx (cdq.screens.main-menu/screen ctx (->background-image)))
     :screens/map-editor      (->stage-screen ctx (cdq.screens.map-editor/screen ctx))
     :screens/minimap         (cdq.screens.minimap/->Screen)
     :screens/options-menu    (->stage-screen ctx (cdq.screens.options-menu/screen ctx (->background-image)))
     :screens/property-editor (->stage-screen ctx (cdq.screens.property-editor/screen ctx (->background-image)))}))
