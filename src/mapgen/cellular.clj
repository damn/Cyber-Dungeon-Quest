(ns mapgen.cellular
  (:require [gdl.graphics.color :as color]
            [gdl.vector :as v])
  (:use data.grid2d
        utils.core
        game.utils.random
        mapgen.utils))

(comment
"The basic idea is to fill the first map randomly, then repeatedly create new maps using the 4-5 rule:
a tile becomes a wall if it was a wall and 4 or more of its nine neighbors were walls,
or if it was not a wall and 5 or more neighbors were.
Put more succinctly, a tile is a wall if the 3x3 region centered on it contained at least 5 walls.
 Each iteration makes each tile more like its neighbors,
and the amount of overall 'noise' is gradually reduced: ")
; http://roguebasin.roguelikedevelopment.org/index.php?title=Cellular_Automata_Method_for_Generating_Random_Cave-Like_Levels

(defn- make-randmap [w h fillprob random]
  (create-grid w h (fn [_] (if (percent-chance fillprob random) :wall :ground))))

(defn- cached-get-adjacent-cells [grid cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (map grid (get-8-neighbour-positions (:position @cell)))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn- do-generation [grid wall-borders? generation]
  (let [k-access (if (zero? generation) :type (dec generation))]
    (doseq [[p cell] grid]
      (when-not (and wall-borders? (border-position? p grid))
        (swap! cell assoc generation
               (if (>
                     (count
                       (filter (fn [cell]
                                 (let [v @cell
                                       value (or (get v k-access) (get v :type))] ; border positions dont update their generations.. so access :type
                                   (or (= :wall value) (nil? value))))
                               (conj (cached-get-adjacent-cells grid cell) cell)))
                     5)
                 :wall
                 :ground))))))

; cool maps: 60/3
; a lot of islands: 70/3
; bigger islands 65/4
; even bigger islands 62/4
; commented because removed defnks
#_(defnks cellular-automata-gridgen
  [w h :fillprob :generations :opt :wall-borders :opt-def :random (java.util.Random.)]
  (let [randmap (make-randmap w h fillprob random)
        grid (if wall-borders
               (assoc-ks randmap (create-borders-positions randmap) :wall)
               randmap)
        grid (transform grid (fn [position value]
                               (atom {:type value
                                      :position position})))]
    (dotimes [n generations]
      (do-generation grid wall-borders n))
    (transform grid (fn [p value] (let [v @value]
                                    (or
                                      (get v (dec generations))
                                      (get v :type))))))) ; border posis dont have generation key

;; regions/connecting TODO different ns. documentation

; commented because removed defnks
#_(defnks flood-fill [grid start :opt :steps :opt-def :label nil]
  (loop [next-positions (if (coll? (first start)) ; todo vector? -> is a position, else do nothing
                          start
                          (list start)) ; collection of posis or just a posi
         labeled []
         labeled-ordered []
         grid grid]
    (if (and (seq next-positions)
             (not (and steps (= (count labeled-ordered) steps))))
      (recur (filter #(= (get grid %) :ground) ; todo fillable?
                     (distinct
                       (mapcat get-8-neighbour-positions next-positions)))
             (concat labeled next-positions)
             (conj labeled-ordered next-positions)
             (assoc-ks grid next-positions label))
      ; todo return a hashmap, also give option for labeling increasing numbers?
      [grid labeled labeled-ordered])))


#_(defn calculate-regions [grid]
  (loop [next-positions (posis grid)
         grid grid
         label 0       ; does not need unique labels -> because if not ground is already unlabeled
         labeled-positions []]
    (if-not (seq next-positions)
      labeled-positions
      (let [posi (first next-positions)
            rest-posis (rest next-positions)]
        (if (= :ground (get grid posi))
          (let [[grid labeled _] (flood-fill grid posi :label label)]
            (recur rest-posis grid (inc label) (conj labeled-positions labeled)))
          (recur rest-posis grid label labeled-positions))))))

(defn- calc-path [[x1 y1] [x2 y2]]
  (concat
    (let [step (if (< x2 x1) -1 1)]
        (for [x (range (+ x1 step) x2 step)] ; + because starting step not part of path
          [x y1]))
      (let [step (if (< y2 y1) -1 1)] ; if no x movement then starting y step here inclusive at path
        (for [y (range y1 y2 step)]
          [x2 y]))))

(defn- make-path [grid [a b] color]
  (assoc-ks grid (calc-path a b) color))

(defn- connect-two-close-regions [grid regions label]
  (let [regions (shuffle regions)
        posi-one (rand-nth (first regions))
        potential-points (map rand-nth (rest regions))
        posi-two (apply min-key #(v/distance posi-one %) potential-points)]
    (make-path grid [posi-one posi-two] label)))

#_(defn rand-connect-two-regions [grid]
  (let [regions (calculate-regions grid)]
    (if-not (> (count regions) 1)
      grid
      (connect-two-close-regions grid regions color/white))))

#_(defn connect-regions [grid]
  (let [regions (calculate-regions grid)
        pairs (loop [regions (zipmap (range) (calculate-regions grid))
                     pairs []]
                (if (> (count regions) 1)
                  (let [label (rand-nth (keys regions))
                        posi-one (rand-nth (get regions label))
                        [closest-label posis] (apply min-key #(v/distance posi-one (rand-nth (% 1)))
                                                     (dissoc regions label))]
                    (recur
                      (dissoc (update-in regions [label] concat posis) closest-label)
                      (conj pairs [posi-one (rand-nth posis)])))
                  pairs))]
    (reduce
      #(make-path %1 %2 :ground)
      grid pairs)))



