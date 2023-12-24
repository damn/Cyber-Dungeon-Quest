(ns screens.main-menu
  (:require [gdl.app :refer [current-context change-screen!]]
            [gdl.context :refer [exit-app draw-centered-image render-gui-view create-image ->text-button key-just-pressed? draw-text ->table ->actor
                                 ->image-widget ->texture-region-drawable]]
            context.cursor
            [gdl.input.keys :as input.keys]
            gdl.screen
            [context.game :refer [start-game-context]]))

(defn screen [context {:keys [bg-image]}]
  (let [table (->table context
                       {:rows [[(->text-button context "Start game"
                                               (fn [_context]
                                                 (swap! current-context start-game-context)
                                                 (change-screen! :screens/game)))]
                               [(->text-button context "Map editor"
                                               (fn [_context]
                                                 (change-screen! :screens/map-editor)))]
                               [(->text-button context "Property editor"
                                               (fn [_context]
                                                 (change-screen! :screens/property-editor)))]
                               [(->text-button context "Exit" exit-app)]]
                        :cell-defaults {:pad-bottom 25}
                        :fill-parent? true})]
    (.center table)
    {:actors [(->image-widget context  ; TODO use visimage, pass directly textureregion
                              (->texture-region-drawable context (:texture (create-image context bg-image)))
                              {})
              ; align = center
              ; scaling =
              table
              (->actor context {:act (fn [ctx]
                                       (when (key-just-pressed? ctx input.keys/escape)
                                         (exit-app ctx)))})
              (context.cursor/->cursor-update-actor context)]}))
