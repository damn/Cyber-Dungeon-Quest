  "Assumption: The map contains no not-allowed diagonal cells, diagonal wall cells where both
  adjacent cells are walls and blocked.
  (important for wavefront-expansion and field-following)"
(ns context.potential-fields
  ; * entities do not move to NADs (they remove them)
  ; * the potential field flows into diagonals, so they should be reachable too.
  ; TODO assert no NADs @ world creation
  (:require [data.grid2d :as grid]
            gdl.context
            [gdl.math.vector :as v]
            [utils.core :refer :all]
            [game.context :refer (get-entities-in-active-content-fields)]
            [game.components.faction :as faction]
            [game.maps.cell-grid :refer (cached-get-adjacent-cells cell-blocked?
                                                                   is-diagonal? inside-cell?
                                                                   fast-cell-blocked?)]))

(defn- occupied-by-other?
  "returns true if there is some solid body with center-tile = this cell
   or a multiple-cell-size body which touches this cell."
  [cell entity]
  (seq (disj (:occupied @cell) entity)))

(def ^:private max-iterations 15)

(defn- fast-is-diagonal? [cell* other-cell*]
  (let [[x1 y1] (:position cell*)
        [x2 y2] (:position other-cell*)]
    (and (not= x1 x2)
         (not= y1 y2))))

; TODO performance
; * cached-adjacent-non-blocked-cells ? -> no need for cell blocked check?
; * sorted-set-by ?
; * do not refresh the potential-fields EVERY frame, maybe very 100ms & check for exists? target if they died inbetween.
; (or teleported?)
(defn- step [cell-grid faction last-marked-cells]
  (let [marked-cells (transient [])
        distance       #(get-in % [faction :distance])
        nearest-entity #(get-in % [faction :entity])
        marked? faction]
    ; sorting important because of diagonal-cell values, flow from lower dist first for correct distance
    (doseq [cell (sort-by #(distance @%) last-marked-cells)
            adjacent-cell (cached-get-adjacent-cells cell-grid cell)
            :let [cell* @cell
                  adjacent-cell* @adjacent-cell]
            :when (not (or (fast-cell-blocked? adjacent-cell*) ; filters nil cells
                           (marked? adjacent-cell*)))
            :let [distance-value (+ (distance cell*)
                                    ; TODO new bottleneck is-diagonal?
                                    (if (fast-is-diagonal? cell* adjacent-cell*)
                                      14 ; square root of 2 * 10
                                      10))]]
      (swap! adjacent-cell assoc faction {:distance distance-value
                                          :entity (nearest-entity cell*)})
      (conj! marked-cells adjacent-cell))
    (persistent! marked-cells)))

(comment

 ; Stepping through manually

 (clear-marked-cells! :good (get @faction->marked-cells :good))

 (defn- faction->tiles->entities-map* [entities]
   (into {}
         (for [[faction entities] (->> entities
                                       (filter   #(:faction @%))
                                       (group-by #(:faction @%)))]
           [faction
            (zipmap (map #(get-tile @%) entities)
                    entities)])))

 (def max-iterations 1)

 (let [entities (map db/get-entity [140 110 91])
       tl->es (:good (faction->tiles->entities-map* entities))
       ]
   tl->es

   (def last-marked-cells (generate-potential-field :good tl->es))
   )
 (println *1)
 (def marked *2)
 (step :good *1)
 )

(defn- generate-potential-field
  "returns the marked-cells"
  [cell-grid faction tiles->entities]
  (let [entity-cell-seq (for [[tile entity] tiles->entities]
                          [entity (get cell-grid tile)])
        marked (map second entity-cell-seq)]
    (doseq [[entity cell] entity-cell-seq]
      (swap! cell assoc faction {:distance 0
                                 :entity entity}))
    (loop [marked-cells     marked
           new-marked-cells marked
           iterations 0]
      (if (= iterations max-iterations)
        marked-cells
        (let [new-marked (step cell-grid faction new-marked-cells)]
          (recur (concat marked-cells new-marked)
                 new-marked
                 (inc iterations)))))))

(defn- tiles->entities [entities faction]
  (let [entities (filter #(= (:faction @%) faction)
                         entities)]
    (zipmap (map #(mapv int (:position @%)) entities)
            entities)))

(def ^:private cache (atom nil))

(defn- update-faction-potential-field [cell-grid faction entities]
  (let [tiles->entities (tiles->entities entities faction)
        last-state   [faction :tiles->entities]
        marked-cells [faction :marked-cells]]
    (when-not (= (get-in @cache last-state) tiles->entities)
      (swap! cache assoc-in last-state tiles->entities)
      (doseq [cell (get-in @cache marked-cells)]
        (swap! cell dissoc faction))
      (swap! cache assoc-in marked-cells (generate-potential-field
                                          cell-grid
                                          faction
                                          tiles->entities)))))

(defn- update-potential-fields* [{:keys [context/world-map] :as context}]
  (let [entities (get-entities-in-active-content-fields context)
        cell-grid (:cell-grid world-map)]
    (doseq [faction [:good :evil]]
      (update-faction-potential-field cell-grid
                                      faction
                                      entities))))

;; MOVEMENT AI

(let [order (grid/get-8-neighbour-positions [0 0])]
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
(defn- filter-viable-cells [entity adjacent-cells]
  (remove-not-allowed-diagonals
    (mapv #(when-not (or (cell-blocked? %)
                         (occupied-by-other? % entity))
             %)
          adjacent-cells)))

(defn- get-min-dist-cell [distance-to cells]
  (when-seq [cells (filter distance-to cells)]
    (apply min-key distance-to cells)))

; rarely called -> no performance bottleneck
(defn- viable-cell? [cell-grid distance-to own-dist entity cell]
  (when-let [best-cell (get-min-dist-cell
                        distance-to
                        (filter-viable-cells entity (cached-get-adjacent-cells cell-grid cell)))]
    (when (< (distance-to best-cell) own-dist)
      cell)))

(defn- find-next-cell
  "returns {:target-entity entity} or {:target-cell cell}. Cell can be nil."
  [cell-grid entity own-cell]
  (let [faction (faction/enemy (:faction @entity))
        distance-to    #(get-in @% [faction :distance])
        nearest-entity #(get-in @% [faction :entity])
        own-dist (distance-to own-cell)
        adjacent-cells (cached-get-adjacent-cells cell-grid own-cell)]
    (if (and own-dist (zero? own-dist))
      {:target-entity (nearest-entity own-cell)}
      (if-let [adjacent-cell (first (filter #(and (distance-to %)
                                                  (zero? (distance-to %)))
                                            adjacent-cells))]
        {:target-entity (nearest-entity adjacent-cell)}
        {:target-cell (let [cells (filter-viable-cells entity adjacent-cells)
                            min-key-cell (get-min-dist-cell distance-to cells)]
                        (cond
                         (not min-key-cell)  ; red
                         own-cell

                         (not own-dist)
                         min-key-cell

                         (> (distance-to min-key-cell) own-dist) ; red
                         own-cell

                         (< (distance-to min-key-cell) own-dist) ; green
                         min-key-cell

                         (= (distance-to min-key-cell) own-dist) ; yellow
                         (or
                          (some #(viable-cell? cell-grid distance-to own-dist entity %) cells)
                          own-cell)))}))))

(extend-type gdl.context.Context
  game.context/PotentialField
  (update-potential-fields [context]
    (update-potential-fields* context))

  ; TODO work with entity* !? occupied-by-other? works with entity not entity* ... not with ids ... hmmm
  (potential-field-follow-to-enemy [{:keys [context/world-map]} entity]
    (let [cell-grid (:cell-grid world-map)
          position (:position @entity)
          own-cell (get cell-grid (mapv int position))
          {:keys [target-entity target-cell]} (find-next-cell cell-grid entity own-cell)]
      (cond
       target-entity
       (v/direction position (:position @target-entity))

       (nil? target-cell)
       nil

       :else
       (when-not (and (= target-cell own-cell)
                      (occupied-by-other? own-cell entity)) ; prevent friction 2 move to center
         (when-not (inside-cell? cell-grid @entity target-cell)
           (v/direction position (:middle @target-cell))))))))

;; DEBUG RENDER TODO not working in old map debug game.maps.render_

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
    (let [occupied-cell (get cell-grid (mapv int (:position @body)))
          own-dist (distance-to occupied-cell)
          adj-cells (cached-get-adjacent-cells cell-grid occupied-cell)
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
