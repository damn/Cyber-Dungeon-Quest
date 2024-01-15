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
            cdq.context.world.render
            [cdq.state.player :as player-state]
            [cdq.state.npc :as npc-state]
            [cdq.world.grid :as world-grid]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.cell :as cell]
            [cdq.entity :as entity]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen))

(defn- on-screen? [entity* {:keys [world-camera world-viewport-width world-viewport-height]}]
  (let [[x y] (:entity/position entity*)
        x (float x)
        y (float y)
        [cx cy] (camera/position world-camera)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float world-viewport-width)  2)))
     (<= ydist (inc (/ (float world-viewport-height) 2))))))

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
  (render-map [{:keys [world-camera] :as ctx}]
    (cdq.context.world.render/render-map ctx (camera/position world-camera)))

  (line-of-sight? [context source* target*]
    (and (:entity/z-order target*)  ; is even an entity which renders something
         (or (not (:entity/player? source*))
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
     {:width w
      :height h
      :cell-blocked-boolean-array (create-cell-blocked-boolean-array grid)
      :content-grid (->content-grid w h 16 16)
      :grid grid
      :explored-tile-corners (atom (grid2d/create-grid w h (constantly false)))}))
  ; FIXME !
  ;(check-not-allowed-diagonals grid)
  )

(defn ->context [ctx]
  (when-let [world (:context/world ctx)]
    (dispose (:tiled-map world)))
  {:context/world (create-world-map (first-level ctx))})

(def ^:private spawn-enemies? true)

(defn transact-create-entities-from-tiledmap! [{:keys [context/world] :as ctx}]
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

