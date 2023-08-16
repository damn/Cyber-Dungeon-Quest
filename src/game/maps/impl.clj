(ns game.maps.impl
  (:require [utils.core :refer [translate-to-tile-middle]]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen))

(defn first-level []
  (let [{:keys [tiled-map start-positions]} (mapgen.module-gen/generate
                                             {:map-size 7
                                              :max-area-level 3
                                              :spawn-rate (/ 20)})
        ;{:keys [end stuff-posis]} (get-populated-grid-posis grid start-posi 3)
        start-position (translate-to-tile-middle
                        (rand-nth (filter #(= "all" (movement-property tiled-map %))
                                          start-positions)))]
    {:map-key :first-level ; necessary ? ->
     :pretty-name "First Level"
     :tiled-map tiled-map
     :start-position start-position
     :load-content (fn []
                     #_(doseq [p stuff-posis]
                         (create-chest p)))
     :rand-item-max-lvl 1
     :spawn-monsters (fn []
                       #_(spawn-monsters
                          techgy-groups
                          :maxno 4
                          :champions false))}))
