(ns game.render
  (:require [gdl.maps.tiled :as tiled]
            [game.protocols :refer [render-debug-before-entities
                                    render-visible-entities
                                    render-debug-after-entities]]
            [game.ui.hp-mana-bars :refer [render-player-hp-mana]]
            [game.maps.tile-color-setters :refer [tile-color-setter]]))

(extend-type gdl.protocols.Context
  game.protocols/GameScreenRender
  (render-world-map [{:keys [context/world-map] :as context}]
    (tiled/render-map context
                      (:tiled-map world-map)
                      #'tile-color-setter))
  (render-in-world-view [c]
    (render-debug-before-entities c)
    (render-visible-entities      c)
    (render-debug-after-entities  c))
  (render-in-gui-view  [c]
    (render-player-hp-mana c)))
