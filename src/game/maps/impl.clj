(nsx game.maps.impl
  (:require (game.maps add data)
            [game.maps.cell-grid :as cell-grid]
            [mapgen.movement-property :refer (movement-property movement-properties)]
            (mapgen module-gen)

            ; no dependencies - load here
            game.entity.chest
            game.entity.door
            ))

; created&loaded in the order defined here
; map with player is first (items need player dependency and maybe more dependencies; also start the game in that map!)
; -> assert?

(comment

 (defn get-player-entity-start-position [tiled-map]
   (let [tile-values (tiled/positions-with-property tiled-map :entities :player)]
     (assert (= (count tile-values) 1))
     (ffirst tile-values)))

 {:start-position (translate-to-tile-middle
                   (get-player-entity-start-position tiled-map))
  :tiled-map tiled-map
  :cell-grid (create-cell-grid-from-tiled-map tiled-map)}

 :cell-grid create-grid-from-gen-grid

 )

; TODO stuff-posis ; etc is using :wall / :ground
; now have :air / :all / nil

; -> spawn_spaces
; mapgen.utils/wall-at? & populate ns

; TODO move everything into tiled-map
; pretty-name / content / start-position / etc
; -> mapgen creates tiledmap and here loaded -> can start the map loaded with editor!
; or make custom tiledmap
; later can save them also ?
(defn first-level []
  (let [{:keys [tiled-map start-positions]} (mapgen.module-gen/generate)
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

; TODO I want to test wizard enemy
; create small tmx map,
; put start position
; put enemy entities
; start game

; or ingame just repl -> spawn wizard @ mouse
