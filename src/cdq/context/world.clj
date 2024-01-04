(ns cdq.context.world
  (:require gdl.disposable
            [gdl.graphics.camera :as camera]
            [gdl.maps.tiled :as tiled]
            [gdl.math.raycaster :as raycaster]
            [gdl.math.vector :as v]
            [data.grid2d :as grid2d]
            [utils.core :refer [->tile tile->middle]]
            [cdq.context.entity.state.player :as player-state]
            [cdq.context.entity.state.npc :as npc-state]
            [cdq.context.world.grid :refer [create-grid]]
            [cdq.context.world.content-grid :refer [->content-grid]]
            [cdq.context :refer [creature-entity ray-blocked? content-grid world-grid get-property]]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.cell :as cell]
            [mapgen.movement-property :refer (movement-property)]
            mapgen.module-gen))

; TODO Check rule context data only accessed in context ns. (even for properties)
; => no dependency on implementation


  ; TODO (:grid world-map) dangerous ! => if multiple maps ! thats why world-map usage limit
  ; ! all context data internal limit & offer protocol !
  ; e.g. id->entity-map ... @ position or @ body/movement check ...

; world map only used @ render tiledmap & minimap & minimap explored tile corners
; => remove so can later add multiple maps if needed
; move content field out
; check usages of grid @ body / potential field / movement where can simplify or collect fns

; rename this to context.world (can have multiple world-maps later)
; (maybe world-map can be a record with functions too ? ...)
; rename grid just to grid
; and protocols also rename folders

; TODO forgot to filter nil cells , e.g. cached-adjcent cells or something

;;

;; just grep context/world-map and move into API

;;;

; TODO world-map context , all access to that data cdq.context fn
; check also other contexts keep private

; ONLY HERE context/world-map !!!
; if multiple maps in 'world => code doesnt change outside protocols

; maybe call context/world ?? world protocol

; ! entities creation fns and audiovisual also cdq.context protocols !
; idk how to call it

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
     (<= xdist (inc (/ world-viewport-width  2)))
     (<= ydist (inc (/ world-viewport-height 2))))))

; TO gdl.math....
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

(extend-type gdl.context.Context
  cdq.context/World
  (line-of-sight? [context source* target*]
    (and (:entity/z-order target*)  ; is even an entity which renders something
         (or (not (:entity/player? source*))
             (on-screen? target* context))
         (not (ray-blocked? context (:entity/position source*) (:entity/position target*)))))

  (ray-blocked? [{:keys [context/world-map]} start target]
    (let [{:keys [cell-blocked-boolean-array width height]} world-map]
      (raycaster/ray-blocked? cell-blocked-boolean-array width height start target)))

  (path-blocked? [context start target path-w]
    (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
      (or
       (ray-blocked? context start1 target1)
       (ray-blocked? context start2 target2))))

  ; TODO put tile param
  (explored? [{:keys [context/world-map] :as context} position]
    (get @(:explored-tile-corners world-map) position))

  ; TODO put tile param already
  (set-explored! [{:keys [context/world-map] :as context} position]
    (swap! (:explored-tile-corners world-map) assoc (->tile position) true))

  (content-grid [{:keys [context/world-map]}]
    (:content-grid world-map))

  (world-grid [{:keys [context/world-map]}]
    (:grid world-map)))

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
    (aset arr
          x
          y
          (boolean (cell/blocked? cell* {:entity/flying? true})))))

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

; looping through all tiles of the map 3 times. but dont do it in 1 loop because player needs to be initialized before all monsters!
(defn- place-entities! [context tiled-map]
  (doseq [[posi creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
    (creature-entity context
                     creature-id
                     (tile->middle posi)
                     {:entity/state (npc-state/->state :sleeping)}))
  ; otherwise will be rendered, is visible
  (tiled/remove-layer! tiled-map :creatures))

(defn- create-entities-from-tiledmap! [{:keys [context/world-map] :as context}]
  (place-entities! context (:tiled-map world-map))
  (creature-entity context
                   :creatures/vampire ; TODO hardcoded
                   (:start-position world-map)
                   {:entity/state (player-state/->state :idle)
                    :entity/player? true
                    :entity/free-skill-points 3
                    :entity/clickable {:type :clickable/player}}))

(defn merge->context [context]
  ; TODO when (:context/world-map context)
  ; dispose (:tiled-map ... )
  ; check if it works
  (let [context (merge context
                       {:context/world-map (create-world-map (first-level context))})]
    (merge context
           {:context/player-entity (create-entities-from-tiledmap! context)})))