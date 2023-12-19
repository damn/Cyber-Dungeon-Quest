(ns app.start
  (:require [gdl.app :as app]
            [gdl.context :refer [generate-ttf]]
            [gdl.default-context :as default-context]
            [app.state :refer [current-context]]
            [context.properties :as properties]
            [game.context :refer [create-gui-stage]]
            game.session ; TODO remove
            game.modifiers.all
            game.components.require-all
            game.effects.require-all
            game.ui.actors
            game.ui.inventory-window
            game.ui.action-bar
            game.ui.hp-mana-bars
            game.tick
            game.render
            game.render.debug
            screens.game
            screens.main-menu
            screens.map-editor
            screens.minimap
            screens.options-menu
            screens.property-editor))

; TODO FIXME HACK !
; change-screen is problematic, changes the current-context atom
; and then the frame finishes with the original unchanged context
; do it always at end of frame
; at manual-tick! just pass as a variable or something or do manual for player-dead !

; contexts == effect
; modifier move to data/ ?
; action-bar / inventory-window / to context ?
; 'maps' => context
; components/ and effects/ out of game ?
; amd modifiers/?
; then no game left ?
; just engine ?

; TODO maybe use safe-merge for all my context stuff (only give warnings @ main-menu when overwriting?)

(defn- create-context []
  (let [context (default-context/->context :tile-size 48)
        properties (let [file "resources/properties.edn"
                         properties (properties/load-edn context file)]
                     (.bindRoot #'properties/properties-file file)
                     (.bindRoot #'properties/properties properties)
                     properties)
        context (merge context {:context/properties properties})]
    (game.ui.inventory-window/initialize! context)
    (game.ui.action-bar/initialize!)
    (game.ui.hp-mana-bars/initialize! context)
    (merge context
           {:default-font (generate-ttf context {:file "exocet/films.EXL_____.ttf"
                                                 :size 16})
            :screens/game            (-> context
                                         (create-gui-stage (game.ui.actors/create-actors context))
                                         screens.game/->Screen)
            :screens/main-menu       (screens.main-menu/screen context {:bg-image "ui/moon_background.png"
                                                                        :skip-main-menu false})
            :screens/map-editor      (screens.map-editor/screen context)
            :screens/minimap         (screens.minimap/->Screen)
            :screens/options-menu    (screens.options-menu/screen context)
            :screens/property-editor (screens.property-editor/screen context)})))

(def app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
         :height 900
         :full-screen? false
         :fps nil} ; TODO fix is set to 60 @ gdl
   :current-context current-context
   :context-fn create-context
   :first-screen :screens/main-menu})

(defn app []
  (app/start app-config))
