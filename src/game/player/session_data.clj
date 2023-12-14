(ns game.player.session-data
  (:require [game.session :as session]
            game.maps.add
            game.maps.impl
            game.maps.data
            game.maps.load
            game.utils.msg-to-player
            game.screens.options
            game.ui.action-bar
            game.ui.inventory-window))

(def state (reify session/State
             (load! [_ _]
               (game.maps.add/add-maps-data
                (game.maps.impl/first-level)))
             (serialize [_])
             (initial-data [_])))

(def ^:private session-components
  [; resets all map data -> do it before creating maps
   game.maps.data/state
   ; create maps before putting entities in them
   game.player.session-data/state
   ; resets all entity data -> do it before addding entities
   game.db/state
   game.ui.inventory-window/state
   ; adding entities (including player-entity)
   game.maps.load/state
   ; now the order of initialisation does not matter anymore
   game.ui.action-bar/state
   game.screens.options/state
   game.ui.mouseover-entity/state
   game.utils.msg-to-player/state])

(defn init []
  (doseq [component session-components]
    (session/load! component
                   (session/initial-data component))))
