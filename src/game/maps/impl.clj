(ns game.maps.impl
  (:require [clojure.edn :as edn]
            [utils.core :refer [translate-to-tile-middle]]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen))

(def map-data-file "resources/maps/map.edn")
; (.readString (gdl.files/internal "maps/map.edn"))
; instead of 'slurp'
; resources automatically included?

(defn first-level []
  (let [{:keys [tiled-map start-positions]} (mapgen.module-gen/generate
                                             (edn/read-string (slurp map-data-file)))
        ;{:keys [end stuff-posis]} (get-populated-grid-posis grid start-posi 3)
        start-position (translate-to-tile-middle ; TODO do @ map load
                        (rand-nth (filter #(= "all" (movement-property tiled-map %))
                                          start-positions)))]
    {:map-key :first-level ; necessary ? ->
     :pretty-name "First Level"
     :tiled-map tiled-map
     :start-position start-position
     :load-content (fn [])
     :rand-item-max-lvl 1
     :spawn-monsters (fn [])}))
