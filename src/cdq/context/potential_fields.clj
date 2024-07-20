(ns cdq.context.potential-fields
  "Assumption: The map contains no not-allowed diagonal cells, diagonal wall cells where both
  adjacent cells are walls and blocked.
  (important for wavefront-expansion and field-following)
  * entities do not move to NADs (they remove them)
  * the potential field flows into diagonals, so they should be reachable too.

  TODO assert @ mapload no NAD's and @ potential field init & remove from
  potential-field-following the removal of NAD's."
  (:require [data.grid2d :as grid2d]
            gdl.context
            [gdl.math.vector :as v]
            [utils.core :refer :all]
            [cdq.api.context :refer (world-grid)]
            [cdq.api.entity :as entity]
            [cdq.api.world.grid :refer [cached-adjacent-cells rectangle->cells]]
            [cdq.api.world.cell :as cell]))

; FIXME assert @ mapload no NAD's and @ potential field init & remove from
; potential-field-following the removal of NAD's.

; TODO remove max pot field movement player screen + 10 tiles as of screen size
; => is coupled to max-steps & also
; to friendly units follow player distance

; TODO remove cached get adj cells & use grid as atom not cells ?
; how to compare perfr ?

; TODO visualize steps, maybe I see something I missed

(comment
 (defrecord Foo [a b c])

 (let [^Foo foo (->Foo 1 2 3)]
   (time (dotimes [_ 10000000] (:a foo)))
   (time (dotimes [_ 10000000] (.a foo)))
   ; .a 7x faster ! => use for faction/distance & make record?
   ))

(comment
 ; Stepping through manually
 (clear-marked-cells! :good (get @faction->marked-cells :good))

 (defn- faction->tiles->entities-map* [entities]
   (into {}
         (for [[faction entities] (->> entities
                                       (filter   #(:entity/faction @%))
                                       (group-by #(:entity/faction @%)))]
           [faction
            (zipmap (map #(entity/tile @%) entities)
                    entities)])))

 (def max-iterations 1)

 (let [entities (map db/get-entity [140 110 91])
       tl->es (:good (faction->tiles->entities-map* entities))]
   tl->es
   (def last-marked-cells (generate-potential-field :good tl->es)))
 (println *1)
 (def marked *2)
 (step :good *1)
 )

(defn- diagonal-direction? [[x y]]
  (and (not (zero? (float x)))
       (not (zero? (float y)))))

(defn- diagonal-cells? [cell* other-cell*]
  (let [[x1 y1] (:position cell*)
        [x2 y2] (:position other-cell*)]
    (and (not= x1 x2)
         (not= y1 y2))))

(defrecord FieldData [distance entity])

(defn- add-field-data! [cell faction distance entity]
  (swap! cell assoc faction (->FieldData distance entity)))

(defn- remove-field-data! [cell faction]  ; don't dissoc - will lose the Cell record type
  (swap! cell assoc faction nil))

; TODO performance
; * cached-adjacent-non-blocked-cells ? -> no need for cell blocked check?
; * sorted-set-by ?
; * do not refresh the potential-fields EVERY frame, maybe very 100ms & check for exists? target if they died inbetween.
; (or teleported?)
(defn- step [grid faction last-marked-cells]
  (let [marked-cells (transient [])
        distance       #(cell/nearest-entity-distance % faction)
        nearest-entity #(cell/nearest-entity          % faction)
        marked? faction]
    ; sorting important because of diagonal-cell values, flow from lower dist first for correct distance
    (doseq [cell (sort-by #(distance @%) last-marked-cells)
            adjacent-cell (cached-adjacent-cells grid cell)
            :let [cell* @cell
                  adjacent-cell* @adjacent-cell]
            :when (not (or (cell/blocked? adjacent-cell*)
                           (marked? adjacent-cell*)))
            :let [distance-value (+ (float (distance cell*))
                                    (float (if (diagonal-cells? cell* adjacent-cell*)
                                             1.4 ; square root of 2 * 10
                                             1)))]]
      (add-field-data! adjacent-cell faction distance-value (nearest-entity cell*))
      (conj! marked-cells adjacent-cell))
    (persistent! marked-cells)))

(defn- generate-potential-field
  "returns the marked-cells"
  [grid faction tiles->entities max-iterations]
  (let [entity-cell-seq (for [[tile entity] tiles->entities] ; FIXME lazy seq
                          [entity (get grid tile)])
        marked (map second entity-cell-seq)]
    (doseq [[entity cell] entity-cell-seq]
      (add-field-data! cell faction 0 entity))
    (loop [marked-cells     marked
           new-marked-cells marked
           iterations 0]
      (if (= iterations max-iterations)
        marked-cells
        (let [new-marked (step grid faction new-marked-cells)]
          (recur (concat marked-cells new-marked) ; FIXME lazy seq
                 new-marked
                 (inc iterations)))))))

(defn- tiles->entities [entities faction]
  (let [entities (filter #(= (:entity/faction @%) faction)
                         entities)]
    (zipmap (map #(entity/tile @%) entities)
            entities)))

(def ^:private cache (atom nil)) ; TODO move to context?

(defn- update-faction-potential-field [grid faction entities max-iterations]
  (let [tiles->entities (tiles->entities entities faction)
        last-state   [faction :tiles->entities]
        marked-cells [faction :marked-cells]]
    (when-not (= (get-in @cache last-state) tiles->entities)
      (swap! cache assoc-in last-state tiles->entities)
      (doseq [cell (get-in @cache marked-cells)]
        (remove-field-data! cell faction))
      (swap! cache assoc-in marked-cells (generate-potential-field
                                          grid
                                          faction
                                          tiles->entities
                                          max-iterations)))))

; TODO main potential field context component create-component params
(def ^:private factions-iterations {:good 5
                                    :evil 15})

(defn- update-potential-fields*! [context entities] ; TODO call on world-grid ?!..
  (let [grid (world-grid context)]
    (doseq [[faction max-iterations] factions-iterations]
      (update-faction-potential-field grid faction entities max-iterations))))

;; MOVEMENT AI

(let [order (grid2d/get-8-neighbour-positions [0 0])]
  (def ^:private diagonal-check-indizes
    (into {} (for [[x y] (filter diagonal-direction? order)]
               [(first (positions #(= % [x y]) order))
                (vec (positions #(some #{%} [[x 0] [0 y]])
                                     order))]))))

(defn- is-not-allowed-diagonal? [at-idx adjacent-cells]
  (when-let [[a b] (get diagonal-check-indizes at-idx)]
    (and (nil? (adjacent-cells a))
         (nil? (adjacent-cells b)))))

(defn- remove-not-allowed-diagonals [adjacent-cells]
  (remove nil?
          (map-indexed
            (fn [idx cell]
              (when-not (or (nil? cell)
                            (is-not-allowed-diagonal? idx adjacent-cells))
                cell))
            adjacent-cells)))

; not using filter because nil cells considered @ remove-not-allowed-diagonals
; TODO only non-nil cells check
; TODO always called with cached-adjacent-cells ...
(defn- filter-viable-cells [entity adjacent-cells]
  (remove-not-allowed-diagonals
    (mapv #(when-not (or (cell/blocked? @%)
                         (cell/occupied-by-other? @% entity))
             %)
          adjacent-cells)))

(defn- get-min-dist-cell [distance-to cells]
  (when-seq [cells (filter distance-to cells)]
    (apply min-key distance-to cells)))

; rarely called -> no performance bottleneck
(defn- viable-cell? [grid distance-to own-dist entity cell]
  (when-let [best-cell (get-min-dist-cell
                        distance-to
                        (filter-viable-cells entity (cached-adjacent-cells grid cell)))]
    (when (< (float (distance-to best-cell)) (float own-dist))
      cell)))

(defn- find-next-cell
  "returns {:target-entity entity} or {:target-cell cell}. Cell can be nil."
  [grid entity own-cell]
  (let [faction (entity/enemy-faction @entity)
        distance-to    #(cell/nearest-entity-distance @% faction)
        nearest-entity #(cell/nearest-entity          @% faction)
        own-dist (distance-to own-cell)
        adjacent-cells (cached-adjacent-cells grid own-cell)]
    (if (and own-dist (zero? (float own-dist)))
      {:target-entity (nearest-entity own-cell)}
      (if-let [adjacent-cell (first (filter #(and (distance-to %)
                                                  (zero? (float (distance-to %))))
                                            adjacent-cells))]
        {:target-entity (nearest-entity adjacent-cell)}
        {:target-cell (let [cells (filter-viable-cells entity adjacent-cells)
                            min-key-cell (get-min-dist-cell distance-to cells)]
                        (cond
                         (not min-key-cell)  ; red
                         own-cell

                         (not own-dist)
                         min-key-cell

                         (> (float (distance-to min-key-cell)) (float own-dist)) ; red
                         own-cell

                         (< (float (distance-to min-key-cell)) (float own-dist)) ; green
                         min-key-cell

                         (= (distance-to min-key-cell) own-dist) ; yellow
                         (or
                          (some #(viable-cell? grid distance-to own-dist entity %) cells)
                          own-cell)))}))))

(defn- inside-cell? [grid entity* cell]
  (let [cells (rectangle->cells grid (:entity/body entity*))]
    (and (= 1 (count cells))
         (= cell (first cells)))))

(extend-type gdl.context.Context ; TODO only on grid this ?!
  cdq.api.context/PotentialField
  (update-potential-fields! [context entities]
    (update-potential-fields*! context entities))

  ; TODO work with entity* !? occupied-by-other? works with entity not entity* ... not with ids ... hmmm
  (potential-field-follow-to-enemy [context entity] ; TODO pass faction here, one less dependency.
    (let [grid (world-grid context)
          position (:entity/position @entity)
          own-cell (get grid (->tile position))
          {:keys [target-entity target-cell]} (find-next-cell grid entity own-cell)]
      (cond
       target-entity
       (v/direction position (:entity/position @target-entity))

       (nil? target-cell)
       nil

       :else
       (when-not (and (= target-cell own-cell)
                      (cell/occupied-by-other? @own-cell entity)) ; prevent friction 2 move to center
         (when-not (inside-cell? grid @entity target-cell)
           (v/direction position (:middle @target-cell))))))))

;; DEBUG RENDER TODO not working in old map debug cdq.maps.render_

; -> render on-screen tile stuff
; -> I just use render-on-map and use tile coords
; -> I need the current viewed tiles x,y,w,h

#_(let [a 0.5]
  (color/defrgb transp-red 1 0 0 a)
  (color/defrgb transp-green 0 1 0 a)
  (color/defrgb transp-orange 1 0.34 0 a)
  (color/defrgb transp-yellow 1 1 0 a))

#_(def ^:private adjacent-cells-colors (atom nil))

#_(defn calculate-mouseover-body-colors [mouseoverbody]
  (when-let [body mouseoverbody]
    (let [occupied-cell (get (world-grid context) (->tile (:entity/position @body)))
          own-dist (distance-to occupied-cell)
          adj-cells (cached-adjacent-cells grid occupied-cell)
          potential-cells (filter distance-to
                                  (filter-viable-cells body adj-cells))
          adj-cells (remove nil? adj-cells)]
      (reset! adjacent-cells-colors
        (genmap adj-cells
          (fn [cell]
            (cond
              (not-any? #{cell} potential-cells)
              transp-red

              (not own-dist) ; die andre hat eine dist da sonst potential-cells rausgefiltert -> besser als jetzige cell.
              transp-green

              (< own-dist (distance-to cell))
              transp-red

              (= own-dist (distance-to cell))
              transp-yellow

              :else transp-green)))))))

#_(defn render-potential-field-following-mouseover-info
    [leftx topy xrect yrect cell mouseoverbody]
    (when-let [body mouseoverbody]
      (when-let [color (get @adjacent-cells-colors cell)]
        (shape-drawer/filled-rectangle leftx topy 1 1 color)))) ; FIXME scale ok for map rendering?
