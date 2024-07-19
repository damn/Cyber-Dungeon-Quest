(ns cdq.screens.main-menu
  (:require [gdl.app :refer [current-context change-screen!]]
            [gdl.context :refer [exit-app ->text-button key-just-pressed? ->table ->actor ->tiled-map]]
            [gdl.input.keys :as input.keys]
            gdl.screen
            [utils.core :refer [safe-get tile->middle]]
            [cdq.context :refer [get-property]]
            [cdq.context.game :refer [start-new-game]]
            mapgen.module-gen))

(defn- ->vampire-tmx [context]
  {:tiled-map (->tiled-map context "maps/vampire.tmx")
   :start-position (tile->middle [32 71])})

(defn- ->rand-module-world [context]
  (let [{:keys [tiled-map
                start-position]} (mapgen.module-gen/generate
                                  context
                                  (get-property context :worlds/first-level))]
    {:tiled-map tiled-map
     :start-position (tile->middle start-position)}))

(defn screen [{:keys [context/config] :as context} background-image]
  (let [table (->table context
                       {:rows (remove nil?
                                      [[(->text-button context "Start vampire.tmx" (fn [context]
                                                                                     (swap! current-context start-new-game (->vampire-tmx context))
                                                                                     (change-screen! :screens/game)))]
                                       [(->text-button context "Start procedural" (fn [context]
                                                                                    (swap! current-context start-new-game (->rand-module-world context))
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
