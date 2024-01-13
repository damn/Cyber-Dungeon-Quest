(ns cdq.context.world
  (:require [gdl.context :refer [render-tiled-map]]
            [gdl.disposable :refer [dispose]]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.color :as color]
            [gdl.maps.tiled :as tiled]
            [gdl.math.raycaster :as raycaster]
            [gdl.math.vector :as v]
            [data.grid2d :as grid2d]
            [utils.core :refer [->tile tile->middle]]
            [cdq.context :refer [explored? transact! transact-all! ray-blocked? content-grid world-grid get-property]]
            [cdq.context.world.grid :refer [create-grid]]
            [cdq.context.world.content-grid :refer [->content-grid]]
            [cdq.state.player :as player-state]
            [cdq.state.npc :as npc-state]
            [cdq.world.grid :as world-grid]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.cell :as cell]
            [cdq.entity :as entity]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen)
  (:import com.badlogic.gdx.graphics.Color))

(defn- set-explored! [{:keys [context/world] :as context} position]
  (swap! (:explored-tile-corners world) assoc (->tile position) true))

(def ^:private explored-tile-color (Color. (float 0.5) (float 0.5) (float 0.5) (float 1)))

(declare ^:private map-render-data)

(defn- set-map-render-data! [{:keys [world-camera] :as ctx}]
  (let [light-position (camera/position world-camera)] ; == player position use ?
    (.bindRoot #'map-render-data [light-position ctx])))

(def ^:private see-all-tiles? false)

(defn- tile-color-setter [_ x y]
  (let [[light-position ctx] map-render-data
        position [x y]
        explored? (explored? ctx position)
        base-color (if explored? explored-tile-color color/black)]
    (if (ray-blocked? ctx light-position position)
      (if see-all-tiles? color/white base-color)
      (do (when-not explored? (set-explored! ctx position))
          color/white))))

#_(defn- on-screen? [entity* {:keys [world-camera world-viewport-width world-viewport-height]}]
  (let [[x y] (:entity/position entity*)
        x (float x)
        y (float y)
        [cx cy] (camera/position world-camera)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ world-viewport-width  2)))
     (<= ydist (inc (/ world-viewport-height 2))))))

; TO gdl.math.... // not tested
(defn- create-double-ray-endpositions
  "path-w in tiles."
  [[start-x start-y] [target-x target-y] path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/get-normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(def ^:private player-los-checks? true)

(extend-type gdl.context.Context
  cdq.context/World
  (render-map [{:keys [context/world] :as ctx}]
    (set-map-render-data! ctx)
    (render-tiled-map ctx (:tiled-map world) tile-color-setter))

  (line-of-sight? [context source* target*]
    (and (:entity/z-order target*)  ; is even an entity which renders something
         #_(or (not (:entity/player? source*)) ; deactivated because performance, also not really needed then
               (on-screen? target* context))
         (not (and player-los-checks?
                   (ray-blocked? context (:entity/position source*) (:entity/position target*))))))

  (ray-blocked? [{:keys [context/world]} start target]
    (let [{:keys [cell-blocked-boolean-array width height]} world]
      (raycaster/ray-blocked? cell-blocked-boolean-array width height start target)))

  (path-blocked? [context start target path-w]
    (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
      (or
       (ray-blocked? context start1 target1)
       (ray-blocked? context start2 target2))))

  ; TODO put tile param
  (explored? [{:keys [context/world] :as context} position]
    (get @(:explored-tile-corners world) position))

  (content-grid [{:keys [context/world]}]
    (:content-grid world))

  (world-grid [{:keys [context/world]}]
    (:grid world)))

(defmethod transact! :tx/add-to-world [[_ entity] ctx]
  (content-grid/update-entity! (content-grid ctx) entity)
  (when (:entity/body @entity)
    (world-grid/add-entity! (world-grid ctx) entity))
  nil)

(defmethod transact! :tx/remove-from-world [[_ entity] ctx]
  (content-grid/remove-entity! (content-grid ctx) entity)
  (when (:entity/body @entity)
    (world-grid/remove-entity! (world-grid ctx) entity))
  nil)

(defmethod transact! :tx/position-changed [[_ entity] ctx]
  (content-grid/update-entity! (content-grid ctx) entity)
  (when (:entity/body @entity)
    (world-grid/entity-position-changed! (world-grid ctx) entity))
  nil)

(defn- first-level [context]
  (let [{:keys [tiled-map
                start-position]} (mapgen.module-gen/generate
                                  context
                                  (get-property context :worlds/first-level))]
    {:map-key :first-level
     :pretty-name "First Level"
     :tiled-map tiled-map
     :start-position (tile->middle start-position)}))

(defn- create-grid-from-tiledmap [tiled-map]
  (create-grid (tiled/width  tiled-map)
               (tiled/height tiled-map)
               (fn [position]
                 (case (movement-property tiled-map position)
                   "none" :none
                   "air"  :air
                   "all"  :all))))

(defn- set-cell-blocked-boolean-array [arr cell*]
  (let [[x y] (:position cell*)]
    (aset arr x y (boolean (cell/blocked? cell* {:entity/flying? true})))))

(defn- create-cell-blocked-boolean-array [grid]
  (let [arr (make-array Boolean/TYPE
                        (grid2d/width grid)
                        (grid2d/height grid))]
    (doseq [cell (grid2d/cells grid)]
      (set-cell-blocked-boolean-array arr @cell))
    arr))

(defn- create-world-map [{:keys [map-key
                                 pretty-name
                                 tiled-map
                                 start-position] :as argsmap}]
  (let [grid (create-grid-from-tiledmap tiled-map)
        w (grid2d/width  grid)
        h (grid2d/height grid)]
    (merge ; TODO no merge, list explicit which keys are there
     (dissoc argsmap :map-key)
     ; TODO here also namespaced keys  !?
     {:width w
      :height h
      :cell-blocked-boolean-array (create-cell-blocked-boolean-array grid)
      :content-grid (->content-grid w h 16 16)
      :grid grid
      ; TODO just explored-tiles? or explored-grid ?
      :explored-tile-corners (atom (grid2d/create-grid w h (constantly false)))})
    )
  ;(check-not-allowed-diagonals grid)
  )

; --> mach prozedural generierte maps mit prostprocessing (fill-singles/set-cells-behind-walls-nil/remove-nads/..?)
;& assertions 0 NADS z.b. ...?

(def ^:private spawn-enemies? true)

(defn- create-entities-from-tiledmap! [{:keys [context/world] :as ctx}]
  (let [tiled-map (:tiled-map world)]
    (when spawn-enemies?
      (transact-all! ctx
                     (for [[posi creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                       [:tx/creature
                        creature-id
                        #:entity {:position (tile->middle posi)
                                  :state (npc-state/->state :sleeping)}])))
    (tiled/remove-layer! tiled-map :creatures)) ; otherwise will be rendered, is visible
  (transact-all! ctx [[:tx/creature
                       :creatures/vampire
                       #:entity {:position (:start-position world)
                                 :state (player-state/->state :idle)
                                 :player? true
                                 :free-skill-points 3
                                 :clickable {:type :clickable/player}}]]))

(defn- fetch-player-entity [ctx]
  {:post [%]}
  (first (filter #(:entity/player? @%)
                 (vals @(:cdq.context.ecs/uids->entities ctx))))) ; TODO private ! move to ecs ! forgot uid change

(require 'cdq.context.transaction-handler)

(comment
 ([:tx/setup-entity 1]
  [:tx/assoc 2]
  [:tx/assoc-in 22]
  [:tx/set-item-image-in-widget 11]
  [:tx/actionbar-add-skill 1]
  [:tx/add-to-world 1])

  ; TODO tx/destroy 'plop' triggers
  ; appearing !
  ; I want to clean up the whole ecs system
  ; => recreate ecs/world context
  ; (Only I don't have rand-gen for map & spawn entities )
  ; that means world-grid,content-grid, ? explored-tiles? (TODO)

  (.postRunnable com.badlogic.gdx.Gdx/app
                 (fn []
                   (let [ctx @gdl.app/current-context
                         entities (vals @(:cdq.context.ecs/uids->entities ctx))
                         txs (deref @#'cdq.context.transaction-handler/txs-coll)]

                     (transact-all! ctx (for [e entities] [:tx/destroy e]))
                     (cdq.context/remove-destroyed-entities! ctx)
                     (cdq.context/rebuild-inventory-widgets ctx)
                     (cdq.context/reset-actionbar ctx)
                     (reset! cdq.context.ecs/id-counter 0)

                     ; apply initial txs
                     (transact-all! ctx txs)

                     ; set up player-entity
                     (swap! gdl.app/current-context merge {:context/player-entity (fetch-player-entity ctx)})
                     nil
                     ))))

(defn merge->context [context]
  (when-let [world (:context/world context)]
    (dispose (:tiled-map world)))
  (let [context (merge context
                       {:context/world (create-world-map (first-level context))})]

    (.bindRoot #'cdq.context.transaction-handler/log-txs? true)
    (reset! @#'cdq.context.transaction-handler/txs-coll [])
    (println "~~ logging txs - " (count @@#'cdq.context.transaction-handler/txs-coll))

    (create-entities-from-tiledmap! context)

    (.bindRoot #'cdq.context.transaction-handler/log-txs? false)
    (println "~~ stop logging txs - " (count @@#'cdq.context.transaction-handler/txs-coll))

    (println "Initial entity txs:")
    (clojure.pprint/pprint
     (for [[txk txs] (group-by first @@#'cdq.context.transaction-handler/txs-coll)]
       [txk (count txs)]))

    (merge context {:context/player-entity (fetch-player-entity context)})))
