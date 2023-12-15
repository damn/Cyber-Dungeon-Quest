; TODO why not just check range to player for updating entities
; why make this fields ?
(ns game.maps.contentfields
  (:require [data.grid2d :as grid]))

; Contentfield Entities
; -> :position sollten sie haben
; Ansonsten updates? / renders? ansonsten ist sinnlos sie dazuzuf�gen.
; TODO entities dont save a contenfield in their :position component but just the idx (for printing..), also simpler here?
(let [field-w 16 ; TODO not world-viewport but player-viewport ? cannot link to world-viewport (minimap ...)
      field-h 16]

  (defn create-mapcontentfields [w h]
    (grid/create-grid (inc (int (/ w field-w))) ; inc wegen r�ndern
                      (inc (int (/ h field-h)))
                      (fn [idx]
                        {:idx idx,
                         :entities (atom #{})})))

  (defn- get-field-idx-of-position [[x y]]
    [(int (/ x field-w))
     (int (/ y field-h))]))

(defn- get-contentfields [{:keys [context/world-map]}]
  (let [cfs  (:contentfields world-map)]
    (println "fetching contentfields: " cfs)
    cfs
    ))

(defn get-content-field [entity]
  (:content-field entity))

(defn remove-entity-from-content-field [entity]
  (swap! (:entities (get-content-field @entity)) disj entity))

(defn put-entity-in-correct-content-field [context entity]
  (let [old-field (get-content-field @entity)
        new-field (get (get-contentfields context)
                       (get-field-idx-of-position (:position @entity)))]
    (when-not (= old-field new-field)
      (swap! (:entities new-field) conj entity)
      (swap! entity assoc-in [:content-field] new-field)
      (when old-field
        (swap! (:entities old-field) disj entity)))))

(defn- get-player-content-field-idx [{:keys [context/player-entity]}]
  (:idx (get-content-field @player-entity)))

(defn get-entities-in-active-content-fields ; ? part of 'gm' ? to be updated/rendered entities ? 'active' ??
  "of current map"
  [context]
  (mapcat #(deref (:entities %)); (comp deref :entities) or #(... %) ?
    (remove nil?
            (map (get-contentfields context)  ; keep (get-contentfields)  ?  also @ potential field thing
                 (let [idx (get-player-content-field-idx)]
                   (cons idx (grid/get-8-neighbour-positions idx)))))))

#_(defn get-all-entities-of-current-map [context]
  (mapcat #(deref (:entities %)) (grid/cells (get-contentfields context))))
