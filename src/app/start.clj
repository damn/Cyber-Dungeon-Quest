(ns app.start
  (:require [gdl.app :as app]
            [gdl.context :refer [generate-ttf ->stage-screen]]
            [context.properties :as properties]
            context.player-message
            context.builder
            context.potential-fields

            game.modifiers.all
            game.components.require-all ; often forget to require, then weird bug ...how to check
            game.effects.require-all
            game.ui.inventory-window
            game.ui.action-bar
            game.ui.hp-mana-bars

            screens.game
            screens.main-menu
            screens.map-editor
            screens.minimap
            screens.options-menu
            screens.property-editor))

; TODO aadd .exit app to context

; DELTA PART OF CONTEXT ?!
; => why we need render AND tick ?!

; TODO use gdx internal files -> context function -> no 'resources/' necessary
; => CONTEXT PROTOCOL READ-FILE GET-FILE !!!
; all lIBGDX STUFF ...

; ALL CONTEXT STUFF MOVE IN GDL/CONTEXT (ecs, effect-handler, modifier-handler (modifier text needs properties), ?)
; => herre only _implementation_ of components/ , effects/, properties/ etc.
; properties/skill faction creature weapon item etc.
; => can make a simple game to show the stuff also
; => incredible...

; effects - properties
(comment
 ; for example for player death  ...
 {:play-sound "sounds/bfxr_playerdeath.wav" ; <- effect-component
  :notify-player "YOU DIED!. Press X to leave."})

; contexts == effect
; modifier move to data/ ?
; action-bar / inventory-window / to context ?
; 'maps' => context
; components/ and effects/ out of game ?
; amd modifiers/?
; then no game left ?
; just engine ?

; TODO maybe use safe-merge for all my context stuff (only give warnings @ main-menu when overwriting?)

(defn- create-context [context]
  (let [context (merge context
                       (properties/->context context "resources/properties.edn"))]
    (game.ui.inventory-window/initialize! context)
    (game.ui.action-bar/initialize!)
    (game.ui.hp-mana-bars/initialize! context)
    (merge context
           ; TODO previous default-font overwritten not dispposed, use safe-merge always.
           {:default-font (generate-ttf context {:file "exocet/films.EXL_____.ttf"
                                                 :size 16})
            :screens/game            (->stage-screen context (screens.game/screen context))
            :screens/main-menu       (->stage-screen context (screens.main-menu/screen context {:bg-image "ui/moon_background.png"}))
            :screens/map-editor      (->stage-screen context (screens.map-editor/screen context))
            :screens/minimap         (screens.minimap/->Screen)
            :screens/options-menu    (->stage-screen context (screens.options-menu/screen context))
            :screens/property-editor (->stage-screen context (screens.property-editor/screen context))})))

(def ^:private tile-size 48)

(def ^:private app-config
  {:app {:title "Cyber Dungeon Quest"
         :width  1440 ; TODO when setting full screen, uses the window size not full w/h, this is MBP full screen w/h
         :height 900
         :full-screen? false
         :fps nil} ; TODO fix is set to 60 @ gdl
   :create-context create-context
   :first-screen :screens/main-menu
   :world-unit-scale (/ tile-size)})

(defn app []
  (app/start app-config))
