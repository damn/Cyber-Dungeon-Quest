(ns game.maps.data
  (:require [data.grid2d :as grid]
            [gdl.lifecycle :as lc]
            [gdl.maps.tiled :as tiled]
            [game.session :as session]
            [game.maps.cell-grid :as cell-grid]
            [game.maps.contentfields :refer [create-mapcontentfields]]))

(declare ^:private world-maps ; map of map-key to the world-map
         added-map-order) ; the map-keys in order of added

(def state (reify session/State
               (load! [_ _]
                 (.bindRoot #'world-maps {})
                 (.bindRoot #'added-map-order []))
               (serialize [_])
               (initial-data [_])))

(defn get-map-data [map-name]
  (get world-maps map-name))

(defn get-pretty-name [map-key]
  (:pretty-name (get-map-data map-key)))

(defn set-map! [new-map]
  {:pre [(contains? world-maps new-map)]}
  (reset! current-map new-map))

(defmacro do-in-map
  "Executes the exprs with current-map set to \"in\" and restores the previous current-map afterwards."
  [in & exprs]
  `(let [old# @current-map
         ~'_ (reset! current-map ~in)
         retrn# (do ~@exprs)]
     (reset! current-map old#)
     retrn#))

(deftype Disposable-State []
  lc/Disposable
  (dispose [_]
    (when (bound? #'world-maps)
      (doseq [[mapkey mapdata] world-maps
              :let [tiled-map (:tiled-map mapdata)]
              :when tiled-map]
        (tiled/dispose tiled-map)))))

(defn add-world-map
  [{:keys [map-key
           pretty-name
           tiled-map
           start-position
           load-content
           rand-item-max-lvl
           spawn-monsters] :as argsmap}]
  {:pre [(not-any? #{map-key} (keys world-maps))]}
  (let [cell-grid (cell-grid/create-grid-from-tiledmap tiled-map)
        w (grid/width  cell-grid)
        h (grid/height cell-grid)
        world-map (merge
                   (dissoc argsmap :map-key)
                   ; TODO here also namespaced keys  !?
                   {:width w
                    :height h
                    :cell-blocked-boolean-array (cell-grid/create-cell-blocked-boolean-array cell-grid)
                    :contentfields (create-mapcontentfields w h)
                    :cell-grid cell-grid
                    :explored-tile-corners (atom (grid/create-grid w h (constantly false)))})]
    (alter-var-root #'world-maps assoc map-key world-map)
    (alter-var-root #'added-map-order conj map-key) )
  ;(check-not-allowed-diagonals cell-grid)
  )

; --> mach prozedural generierte maps mit prostprocessing (fill-singles/set-cells-behind-walls-nil/remove-nads/..?)
;& assertions 0 NADS z.b. ...?
