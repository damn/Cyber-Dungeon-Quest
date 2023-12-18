(ns game.session
  (:require [clojure.edn :as edn]
            [data.grid2d :as grid]
            [gdl.app :as app]
            gdl.protocols
            [gdl.maps.tiled :as tiled]
            [gdl.scene2d.stage :as stage]
            [utils.core :refer [translate-to-tile-middle]]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen
            [game.maps.cell-grid :as cell-grid]
            [game.maps.contentfields :refer [create-mapcontentfields]]
            game.protocols
            [context.ecs :as ecs]
            [game.entities.creature :as creature-entity]
            game.ui.action-bar
            game.ui.inventory-window)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.audio.Sound
           com.badlogic.gdx.scenes.scene2d.Stage))

(extend-type gdl.protocols.Context
  game.protocols/Context
  (play-sound! [{:keys [assets]} file]
    (.play ^Sound (get assets file)))

  (show-msg-to-player! [_ message]
    (println message))

  game.protocols/StageCreater
  (create-gui-stage [{:keys [gui-viewport batch]} actors]
    (let [stage (stage/create gui-viewport batch)]
      (doseq [actor actors]
        (.addActor stage actor))
      stage))

  game.protocols/ContextStageSetter
  (set-screen-stage [_ stage]
    ; set-screen-stage & also sets it to context ':stage' key
    (.setInputProcessor Gdx/input stage))
  (remove-screen-stage [_]
    ; TODO dissoc also :stage from context
    (.setInputProcessor Gdx/input nil)))

(extend-type Stage
  gdl.protocols/Disposable
  (dispose [stage]
    (.dispose stage))
  game.protocols/Stage
  (draw [stage]
    (.draw stage))
  (act [stage delta]
    (.act stage delta)))
; TODO also for my own Disposable protocol

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
  gdl.protocols/Disposable
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

  ; TODO make namespaced kws / or variables ? no then deps.
  (let [context (merge context
                       (ecs/->context :z-orders [:on-ground ; items
                                                 :ground    ; creatures, player
                                                 :flying    ; flying creatures
                                                 :effect])  ; projectiles, nova
                       {:context/world-map (create-world-map (first-level (:context/properties context)))
                        :context/running (atom true)
                        :context/mouseover-entity (atom nil)}) ; references nil or an atom, so need to deref it always and check
        player-entity (create-entities-from-tiledmap! context)
        context (assoc context :context/player-entity player-entity)]
    ; TODO take care nowhere is @app/state called or app/current-context
    ; so we use the new context here

    (game.ui.action-bar/reset-skills!)
    context))
