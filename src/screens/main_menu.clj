(ns screens.main-menu
  (:require [gdl.app :refer [current-context change-screen!]]
            [gdl.context :refer [exit-app draw-centered-image render-gui-view create-image ->text-button key-just-pressed? draw-text ->table ->actor ->image-widget]]
            [gdl.input.keys :as input.keys]
            gdl.screen
            [utils.core :refer [safe-get]]
            [context.game :refer [start-game-context]]))

(defn screen [{:keys [context/config] :as context} {:keys [bg-image]}]
  (let [table (->table context
                       {:rows [[(->text-button context "Start game" (fn [_context]
                                                                      (swap! current-context start-game-context)
                                                                      (change-screen! :screens/game)))]

                               [(when (safe-get config :map-editor?)
                                  (->text-button context "Map editor" (fn [_context]
                                                                        (change-screen! :screens/map-editor))))]
                               [(when (safe-get config :property-editor?)
                                  (->text-button context "Property editor" (fn [_context]
                                                                             (change-screen! :screens/property-editor))))]

                               [(->text-button context "Exit" exit-app)]]
                        :cell-defaults {:pad-bottom 25}
                        :fill-parent? true})]
    (.center table)
    ; => reuse image @ options menu / property ?
    {:actors [(let [image (->image-widget context (create-image context bg-image) {})]
                (.setScaling image com.badlogic.gdx.utils.Scaling/fill)
                (.setAlign image com.badlogic.gdx.utils.Align/center)
                (.setFillParent image true)
                image)
              ; setScaling image com.badlogic.gdx.utils.Scaling/fill
              ; align = center
              ; scaling =
              ; add opts for this
              ; => add also opts for the drawable thing itself then ? no not necessary i guess
              table
              (->actor context {:act (fn [ctx]
                                       (when (key-just-pressed? ctx input.keys/escape)
                                         (exit-app ctx)))})]}))
