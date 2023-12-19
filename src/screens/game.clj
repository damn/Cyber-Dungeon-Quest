(ns screens.game
  (:require [gdl.context :refer [render-gui-view render-world-view]]
            [gdl.screen :refer [Screen]]
            [gdl.maps.tiled :as tiled]
            [game.context :refer [render-visible-entities]]
            [game.tick :refer [tick-game]]
            game.ui.actors
            [game.ui.hp-mana-bars :refer [render-player-hp-mana]]
            [game.maps.tile-color-setters :refer [tile-color-setter]]
            [game.render.debug :as debug]))

(defrecord SubScreen []
  Screen
  (show [_ _context])
  (hide [_ _context])
  (render [_ {:keys [context/world-map] :as context}]
    (tiled/render-map context
                      (:tiled-map world-map)
                      #'tile-color-setter)
    (render-world-view context
                       (fn [c]
                         (debug/render-before-entities c)
                         (render-visible-entities      c)
                         (debug/render-after-entities  c)))
    (render-gui-view context render-player-hp-mana))
  (tick [_ context delta]
    (tick-game context delta)))

(defn screen [context]
  {:actors (game.ui.actors/create-actors context)
   :sub-screen (screens.game/->SubScreen)})
