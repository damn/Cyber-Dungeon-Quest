(ns mapgen.spawn-spaces
  (:use data.grid2d
        utils.core
        mapgen.utils
        game.maps.cell-grid))

(defn- ground?
  "used so that get-spawn-spaces / get-spawn-space works for randgen-maps and realized maps"
  [cell]
  (if cell
    (if (keyword? cell)
      (= :ground cell)
      (not (cell-blocked? cell)))))

(defn- is-3x3-space? [grid position]
  (every? ground? (get-3x3-cellvalues grid position)))

(defn- get-rand-3x3-space [quadranten-posis grid]
  (when quadranten-posis
    (first
      (filter #(is-3x3-space? grid %) (shuffle quadranten-posis)))))

(defn- get-spawn-spaces
  "partitions the grid in fields and tries to find a 'space' in every field."
  [grid]
  (let [field-w 13 ; displ-w-in-tiles ; TODO adjust to screen-size
        field-h 10 ; displ-h-in-tiles
        get-field-of-posi (fn [[x y]] [(int (/ x field-w))
                                       (int (/ y field-h))])
        ; grid that tells for every position in original grid the field-position index
        field-posis-grid (create-grid (width grid) (height grid) get-field-of-posi)
        ; now create for every cell/quadrant in the fields the seq of positions
        quadranten-posis (atom {})]
    (doseq [[posi quadrant] field-posis-grid]
      (swap! quadranten-posis update-in [quadrant] conj posi))
    (remove nil?
      (map #(get-rand-3x3-space % grid)
        (vals @quadranten-posis)))))

(defn get-spawn-positions-groups
  "Returns a coll of positions and their adjacent-positions == 9 positions every item"
  [grid]
  (map #(cons % (get-8-neighbour-positions %))
       (get-spawn-spaces grid)))

(defn mark-spawn-spaces [grid]
  (assoc-ks grid (get-spawn-spaces grid) :room))

(defn mark-spaces [grid]
  (let [rooms (filter #(is-3x3-space? grid %) (posis grid))]
    (assoc-ks grid rooms :room)))
