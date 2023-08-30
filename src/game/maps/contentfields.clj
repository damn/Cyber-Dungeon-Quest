; TODO  nothing to do with maps - move to position ?

; TODO why not just check range to player for updating entities
; why make this fields ?
(ns game.maps.contentfields
  (:require [data.grid2d :as grid]
            [utils.core :refer [assoc-in!]]
            [game.maps.data :refer (get-current-map-data)]
            [game.player.entity :refer (player-entity)]))

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

(defn- get-contentfields []
  (:contentfields (get-current-map-data)))

(defn get-content-field [entity]
  (:content-field entity))

(defn remove-entity-from-content-field [entity]
  (swap! (:entities (get-content-field @entity)) disj entity))

(defn put-entity-in-correct-content-field [entity]
  (let [old-field (get-content-field @entity)
        new-field (get (get-contentfields)
                       (get-field-idx-of-position (:position @entity)))]
    (when-not (= old-field new-field)
      (swap! (:entities new-field) conj entity)
      (assoc-in! entity [:content-field] new-field)
      (when old-field
        (swap! (:entities old-field) disj entity)))))

(defn get-player-content-field-idx []
  (:idx (get-content-field @player-entity)))

(defn get-entities-in-active-content-fields
  "of current map"
  []
  (mapcat #(deref (:entities %)); (comp deref :entities) or #(... %) ?
    (remove nil?
            (map (get-contentfields)  ; keep (get-contentfields)  ?  also @ potential field thing
                 (let [idx (get-player-content-field-idx)]
                   (cons idx (grid/get-8-neighbour-positions idx)))))))

(defn get-all-entities-of-current-map []
  (mapcat #(deref (:entities %)) (grid/cells (get-contentfields))))
