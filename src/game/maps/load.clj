(ns game.maps.load
  (:require [gdl.tiled :as tiled]
            [utils.core :refer [translate-to-tile-middle]]
            [game.session :as session]
            [game.maps.data :as data]
            [game.entities.creature :as creature-entity]
            game.player.core))

; looping through all tiles of the map 3 times. but dont do it in 1 loop because player needs to be initialized before all monsters!
(defn- place-entities [tiled-map]
  (doseq [[posi creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
    (creature-entity/create! creature-id
                             (translate-to-tile-middle posi)
                             {:sleeping true}))
  ; otherwise will be rendered, is visible, can also just setVisible layer false
  (tiled/remove-layer! tiled-map :creatures))

(defn load-maps-content
  "loads the map content in the right order"
  []
  ; TODO assert spawns !!
  (creature-entity/create! :vampire ; <+ this main game config should not be here spread out through the code (also skills damage tc.)
                           (:start-position (data/get-current-map-data))
                           {:is-player true})
  (doseq [map-name data/added-map-order]
    (data/do-in-map map-name
                    (let [{:keys [load-content spawn-monsters tiled-map]} (data/get-map-data map-name)]
                      (when tiled-map ; TODO always
                        (place-entities tiled-map))
                      (load-content) ; TODO use place-entities
                      (when spawn-monsters ; TODO use place-entities
                        (spawn-monsters))))
    ;(log "Loaded " map-name " content!")
    ))


(def state (reify session/State
             (load! [_ mapkey]
               (data/set-map! mapkey)
               (load-maps-content))
             (serialize [_]
               @data/current-map)
             (initial-data [_]
               (first data/added-map-order))))






