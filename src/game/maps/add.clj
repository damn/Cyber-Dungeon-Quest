(ns game.maps.add
  (:require [data.grid2d :as grid]
            [game.maps.data :as data]
            [game.maps.cell-grid :as cell-grid]
            [game.maps.contentfields :refer [create-mapcontentfields]]))

(defn add-maps-data
  [{:keys [map-key
           pretty-name
           tiled-map
           start-position
           load-content
           rand-item-max-lvl
           spawn-monsters] :as argsmap}]
  {:pre [(not-any? #{map-key} (data/get-map-keys))]}
  (let [cell-grid (cell-grid/create-grid-from-tiledmap tiled-map)
        w (grid/width  cell-grid)
        h (grid/height cell-grid)]
    (data/add-map map-key
                  (merge
                   (dissoc argsmap :map-key)
                   {:cell-blocked-boolean-array (cell-grid/create-cell-blocked-boolean-array cell-grid)
                    :contentfields (create-mapcontentfields w h)
                    :cell-grid cell-grid
                    :explored-tile-corners (atom (grid/create-grid w h (constantly false)))})))
  ;(log "Finished creating " map-key)
  ;(check-not-allowed-diagonals cell-grid)
  )

; --> mach prozedural generierte maps mit prostprocessing (fill-singles/set-cells-behind-walls-nil/remove-nads/..?)
;& assertions 0 NADS z.b. ...?
