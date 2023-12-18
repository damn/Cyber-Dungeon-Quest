(ns game.session
  (:require [clojure.edn :as edn]
            [x.x :refer [update-map doseq-entity]]
            [data.grid2d :as grid]
            [gdl.app :as app]
            [gdl.lifecycle :as lc]
            [gdl.maps.tiled :as tiled]
            [utils.core :refer [translate-to-tile-middle]]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen
            [game.maps.cell-grid :as cell-grid]
            [game.maps.contentfields :refer [create-mapcontentfields]]
            [game.context :as gm]
            [game.entity :as entity]
            [game.entities.creature :as creature-entity]
            game.ui.action-bar
            game.ui.inventory-window)
  (:import com.badlogic.gdx.audio.Sound))

; not sure I should extend gdl.app.Context
; or create a 'session' inside , why e need the whole context???
; so far only ids->entities? which could be part of some kind of 'session' or 'world' context/world
; record and not part of the overall record ?!
; => see create! and destroy! which needs it ...


; :body => needs map data / cell-grid for create! / destroy!
; :id needs id-entity-map
; items need properties and try-pickup-item!, maybe problematic
; :position needs contentfields
; :skills need properties

; player sets world-camera position , also at tick (I could do that before render just set the camera position to player entity)
; :default-monster-death needs audiovisual -> sound -> assets/properties

; what do I want to do ? completely immutable game structure, only pure functions?
; => transactions on the game state itself are done in tick, no atoms ...

(extend-type gdl.app.Context
  gm/Context
  (get-entity [{:keys [context/ids->entities]} id]
    (get @ids->entities id))

  (entity-exists? [context e]
    (gm/get-entity context (:id @e)))

  (create-entity! [context components-map]
    {:pre [(not (contains? components-map :id))]}
    (-> (assoc components-map :id nil)
        (update-map entity/create)
        atom
        (doseq-entity entity/create! context)))

  (destroy-to-be-removed-entities!
    [{:keys [context/ids->entities] :as context}]
    (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
            :when (gm/entity-exists? context e)] ; TODO why is this ?, maybe assert ?
      (swap! e update-map entity/destroy)
      (doseq-entity e entity/destroy! context)))

  (play-sound! [{:keys [assets]} file]
    (.play ^Sound (get assets file)))

  (show-msg-to-player! [_ message]
    (println message)))

(defn- first-level [properties]
  (let [{:keys [tiled-map start-positions]} (mapgen.module-gen/generate
                                             ; TODO move to properties
                                             (assoc (edn/read-string (slurp "resources/maps/map.edn"))
                                                    :creature-properties (filter :species (vals properties))))
        start-position (translate-to-tile-middle
                        (rand-nth (filter #(= "all" (movement-property tiled-map %))
                                          start-positions)))]
    {:map-key :first-level
     :pretty-name "First Level"
     :tiled-map tiled-map
     :start-position start-position}))

(defn- create-world-map [{:keys [map-key
                                 pretty-name
                                 tiled-map
                                 start-position] :as argsmap}]
  (let [cell-grid (cell-grid/create-grid-from-tiledmap tiled-map)
        w (grid/width  cell-grid)
        h (grid/height cell-grid)]
    (merge ; TODO no merge, list explicit which keys are there
     (dissoc argsmap :map-key)
     ; TODO here also namespaced keys  !?
     {:width w
      :height h
      :cell-blocked-boolean-array (cell-grid/create-cell-blocked-boolean-array cell-grid)
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

(defn- create-entities-from-tiledmap! [{:keys [context/world-map] :as context}]

  ; TODO they need player entity context ?!
  (place-entities! context (:tiled-map world-map))

  (let [player-entity  (creature-entity/create! :vampire
                                                (:start-position world-map)
                                                {:is-player true}
                                                context)]
    player-entity))

(deftype Disposable-State []
  lc/Disposable
  (dispose [_]
    ; TODO dispose tiledmap of context/world-map => make disposable record
    ; TODO dispose maps when starting a new session
    ; => newly loaded tiledmaps
    #_(when (bound? #'world-maps)
      (doseq [[mapkey mapdata] world-maps
              :let [tiled-map (:tiled-map mapdata)]
              :when tiled-map]
        (tiled/dispose tiled-map)))))

(defn init-context [context]
  (game.ui.inventory-window/rebuild-inventory-widgets!) ; before adding entities ( player gets items )
  (let [context (merge context
                       {:context/ids->entities (atom {})
                        :context/world-map (create-world-map (first-level (:context/properties context)))
                        :context/running (atom true)
                        :context/mouseover-entity (atom nil)}) ; references nil or an atom, so need to deref it always and check
        player-entity (create-entities-from-tiledmap! context)
        context (assoc context :context/player-entity player-entity)]
    ; TODO take care nowhere is @app/state called or app/current-context
    ; so we use the new context here

    (game.ui.action-bar/reset-skills!)
    context))
