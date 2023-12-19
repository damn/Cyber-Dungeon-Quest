(ns context.world-map
  (:require [clojure.edn :as edn]
            [gdl.maps.tiled :as tiled]
            [data.grid2d :as grid]
            [gdl.context :refer [all-properties]]
            gdl.disposable
            [utils.core :refer [translate-to-tile-middle]]
            [game.maps.contentfields :refer [create-mapcontentfields]]
            [game.maps.cell-grid :as cell-grid]
            [game.entities.creature :as creature-entity]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen))

; TODO world-map context , all access to that data game.context fn
; check also other contexts keep private

; ONLY HERE context/world-map !!!
; if multiple maps in 'world => code doesnt change outside protocols

; maybe call context/world ?? world protocol

; ! entities creation fns and audiovisual also game.context protocols !
; idk how to call it

(defn- first-level [context]
  (let [{:keys [tiled-map start-positions]} (mapgen.module-gen/generate
                                             ; TODO move to properties
                                             (assoc (edn/read-string (slurp "resources/maps/map.edn"))
                                                    :creature-properties (all-properties context :species)))
        start-position (translate-to-tile-middle
                        (rand-nth (filter #(= "all" (movement-property tiled-map %))
                                          start-positions)))]
    {:map-key :first-level
     :pretty-name "First Level"
     :tiled-map tiled-map
     :start-position start-position}))

(defrecord Cell [position
                 middle
                 adjacent-cells
                 movement
                 entities
                 occupied
                 good
                 evil])

(defn- create-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (atom
   (map->Cell
    {:position position
     :middle (translate-to-tile-middle position)
     :movement movement
     :entities #{}
     :occupied #{}})))

(defn- create-grid-from-tiledmap [tiled-map]
  (grid/create-grid (tiled/width  tiled-map)
                    (tiled/height tiled-map)
                    (fn [position]
                      (create-cell position
                                   (case (movement-property tiled-map position)
                                     "none" :none
                                     "air"  :air
                                     "all"  :all)))))

(defn- set-cell-blocked-boolean-array [arr cell]
  (let [[x y] (:position @cell)]
    (aset arr
          x
          y
          (boolean (cell-grid/cell-blocked? cell {:is-flying true})))))

(defn- create-cell-blocked-boolean-array [grid]
  (let [arr (make-array Boolean/TYPE
                        (grid/width grid)
                        (grid/height grid))]
    (doseq [cell (grid/cells grid)]
      (set-cell-blocked-boolean-array arr cell))
    arr))

(defn- create-world-map [{:keys [map-key
                                 pretty-name
                                 tiled-map
                                 start-position] :as argsmap}]
  (let [cell-grid (create-grid-from-tiledmap tiled-map)
        w (grid/width  cell-grid)
        h (grid/height cell-grid)]
    (merge ; TODO no merge, list explicit which keys are there
     (dissoc argsmap :map-key)
     ; TODO here also namespaced keys  !?
     {:width w
      :height h
      :cell-blocked-boolean-array (create-cell-blocked-boolean-array cell-grid)
      :contentfields (create-mapcontentfields w h)
      :cell-grid cell-grid
      :explored-tile-corners (atom (grid/create-grid w h (constantly false)))})
    )
  ;(check-not-allowed-diagonals cell-grid)
  )

; --> mach prozedural generierte maps mit prostprocessing (fill-singles/set-cells-behind-walls-nil/remove-nads/..?)
;& assertions 0 NADS z.b. ...?


; looping through all tiles of the map 3 times. but dont do it in 1 loop because player needs to be initialized before all monsters!
(defn- place-entities! [context tiled-map]
  (doseq [[posi creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
    (creature-entity/create! creature-id
                             (translate-to-tile-middle posi)
                             {:initial-state :sleeping}
                             context))
  ; otherwise will be rendered, is visible, can also just setVisible layer false
  (tiled/remove-layer! tiled-map :creatures))

; PROTOCOL FN ?
(defn- create-entities-from-tiledmap! [{:keys [context/world-map] :as context}]
  ; TODO they need player entity context ?!
  (place-entities! context (:tiled-map world-map))
  (creature-entity/create! :vampire
                           (:start-position world-map)
                           {:is-player true}
                           context))

(deftype Disposable-State [] ; TODO let world-map record implement this so tiledmaps get disposed
  gdl.disposable/Disposable
  (dispose [_]
    ; TODO dispose tiledmap of context/world-map => make disposable record
    ; TODO dispose maps when starting a new session
    ; => newly loaded tiledmaps
    #_(when (bound? #'world-maps)
      (doseq [[mapkey mapdata] world-maps
              :let [tiled-map (:tiled-map mapdata)]
              :when tiled-map]
        (tiled/dispose tiled-map)))))

(defn merge->context [context]
  (let [context (merge context
                       {:context/world-map (create-world-map (first-level context))})]
    (merge context
           {:context/player-entity (create-entities-from-tiledmap! context)})))


; TODO extend here game.context ( also properties do  )
; search world-map
; * get-bodies-at-position
; * valid-position?
; * touched-cells/occupied cells ??
; * update posi projectile also does something (like valid position)
; * sleeping get-friendly-entities-in-line-of-sight (et entities in circle)
; * nearest-enemy-entity
; * potential field direction to nearest enemy
; * npc sleeping state tick! check distance nearest enemy
; * projectile-path-blocked?
; * in-line-of-sight?

; move game.maps code here probably mostly (ray-blocked? ...)
; contentfields
; potential field ? part of this context?
; explored tiles set / check
; ray blocked @ tile-setter
; geom tests ?!

; * render tiledmap (context/world-map => :tiled-map)
; => render-world
; * render-minimap ?!
