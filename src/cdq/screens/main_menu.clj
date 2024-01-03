(ns cdq.screens.main-menu
  (:require [gdl.app :refer [current-context change-screen!]]
            [gdl.context :refer [exit-app ->text-button key-just-pressed? ->table ->actor]]
            [gdl.input.keys :as input.keys]
            gdl.screen
            [utils.core :refer [safe-get]]
            [cdq.context.game :refer [start-game-context]]))

(defn screen [{:keys [context/config] :as context} background-image]
  (let [table (->table context
                       {:rows (remove nil?
                                      [[(->text-button context "Start game" (fn [_context]
                                                                              (swap! current-context start-game-context)
                                                                              (change-screen! :screens/game)))]

                                       (when (safe-get config :map-editor?)
                                         [(->text-button context "Map editor" (fn [_context]
                                                                                (change-screen! :screens/map-editor)))])
                                       (when (safe-get config :property-editor?)
                                         [(->text-button context "Property editor" (fn [_context]
                                                                                     (change-screen! :screens/property-editor)))])

                                       [(->text-button context "Exit" exit-app)]])
                        :cell-defaults {:pad-bottom 25}
                        :fill-parent? true})]
    {:actors [background-image
              table
              (->actor context {:act (fn [ctx]
                                       (when (key-just-pressed? ctx input.keys/escape)
                                         (exit-app ctx)))})]}))
