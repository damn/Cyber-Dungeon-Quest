; TODO what is happening here ?
; somehow looking for places far awa from player start position and placing treasure/?
; -> ns docs.
(ns mapgen.populate
  (:use
    game.utils.random
    (mapgen cellular
            [utils :only (wall-at?)]
            #_[findpath :only (find-path)])))

#_(defn get-rand-end-posi [grid start]
  (let [[_ _ labeled-ordered] (flood-fill grid start)]
    ; rest because start-posi should not marked as end-posi
    (rand-nth (high-weighted-rand-nth (rest labeled-ordered)))))

; TODO beim pfad genau nebeneinander die stuff-posis..
#_(defn get-populated-grid-posis [grid start-posi no-stuff]
  (let [end-posi (get-rand-end-posi grid start-posi)
        path-posis (set ; for checking.. if contains?
                     (find-path start-posi end-posi grid wall-at?))
        [_ _ labeled-ordered] (flood-fill grid path-posis)
        stuff-posis (loop [labeled-ordered (rest labeled-ordered) ; dont put stuff directly on the path (first element)
                           stuff-cnt no-stuff
                           stuff-posis []]
                      (if (or (zero? stuff-cnt) (empty? labeled-ordered))
                        stuff-posis
                        (let [stuff-position (rand-nth (high-weighted-rand-nth labeled-ordered))
                              [_ posis _] (flood-fill grid stuff-position :steps 24) ; TODO flood-fill with 8 instead of 4 neighbours?
                              need-to-be-removed (set posis)
                              labeled-ordered (remove empty? (map (fn [posis]
                                                                    (remove #(contains? need-to-be-removed %) posis))
                                                                  labeled-ordered))]
                          (when (contains? path-posis stuff-position)
                            ; triggered at the end of a loop!
                            (println "stuff-position on path-posi - should not be !!"))
                          (recur labeled-ordered
                                 (dec stuff-cnt)
                                 (conj stuff-posis stuff-position)))))]
    {:end end-posi
     :path-posis path-posis
     :stuff-posis stuff-posis}))
