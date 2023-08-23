(nsx game.components.position
  (:require [game.maps.contentfields :refer (put-entity-in-correct-content-field
                                             remove-entity-from-content-field)]))

; TODO content-field component !!!

; => this is same like cell/grid/id->entity-map/contentfields
; -> @ entity creation 'add to db'

; TODO merge with cell-grid-connections & assume all entities have body
; create-world-references!
; update-world-references!
; delete-world-references!
(defcomponent :position p
  (db/create!  [_ e] (put-entity-in-correct-content-field e))  ;cf/put
  (db/destroy! [_ e] (remove-entity-from-content-field    e))  ;cf/remove
  (moved!   [_ e] (put-entity-in-correct-content-field e))) ;cf/put

; TODO use mapv int :position ? clearer..
(def get-tile (comp int-posi :position))
