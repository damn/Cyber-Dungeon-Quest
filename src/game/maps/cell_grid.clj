(ns game.maps.cell-grid
  (:require [gdl.math.geom :as geom]
            [data.grid2d :as grid]))

; Make cell & cell-grid protocols
; used @ body, movement, potential-fields
; body maybe just cell-grid/add-entity cell-grid/remove-entity

; simple! => grep cell-grid or context/world-map
; would be nice to have an API for it ....
; and context/world separate context/world/cell-grid context/world/content-field etc.
; simple context/world/explored

(defn cell-blocked?
  ([cell]
   (cell-blocked? cell {}))
  ([cell {:keys [is-flying]}]
   (or (nil? cell)
       (case (:movement @cell)
         :none true
         :air (not is-flying)
         :all false))))

(defn fast-cell-blocked? [cell*]
  (or (nil? cell*)
      (case (:movement cell*)
        :none true
        :air true
        :all false)))

(defn get-entities [cell]
  (if cell
    (:entities @cell)
    #{}))

(defn get-entities-from-cells [cells]
  (distinct (mapcat get-entities cells)))

(defn- rectangle->touched-tiles
  [{[x y] :left-bottom :keys [left-bottom width height]}]
  {:pre [left-bottom width height]}
  (let [x       (float x)
        y       (float y)
        width   (float width)
        height  (float height)
        l (int x)
        b (int y)
        r (int (+ x width))
        t (int (+ y height))]
    (distinct
     (if (or (> width 1) (> height 1))
       (for [x (range l (inc r))
             y (range b (inc t))]
         [x y])
       [[l b] [l t] [r b] [r t]]))))

(defn rectangle->touched-cells [cell-grid rectangle]
  (->> rectangle
       rectangle->touched-tiles
       (map cell-grid)))

(defn circle->touched-cells [cell-grid circle]
  (->> circle
       geom/circle->outer-rectangle
       (rectangle->touched-cells cell-grid)))
