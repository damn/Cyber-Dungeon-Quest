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
  (create!  [_ e] (put-entity-in-correct-content-field e))  ;cf/put
  (destroy! [_ e] (remove-entity-from-content-field    e))  ;cf/remove
  (moved!   [_ e] (put-entity-in-correct-content-field e))) ;cf/put

(defcomponent :children children
  (destroy! [_ entity]
    #_(if-let [children (:children @r)]
      ; TODO destroy all children & test // or get rid of parent/children
      )
    )
  (moved! [_ entity]
    (let [position (:position @entity)]
      (doseq [child children]
        (swap! child assoc :position position)))))

; TODO stun-effect component <- render-below
; TODO string-effect just render-effect ? (over anything ?)!
; first destroy entity, then not necessary for children to remove themself anymore @ parent :children
(defcomponent :parent parent
  (create! [_ child]
    (assert (exists? parent))
    (if-let [children (:children @parent)]
      (do
       (assert (not (contains? children child)))
       (swap! parent update :children conj child))
      (swap! parent assoc :children #{child})))
  (destroy! [_ child]
    (when (exists? parent)
      (let [children (:children @parent)]
        (assert (contains? children child))
        (if (= children #{child})
          (swap! parent dissoc :children)
          (swap! parent update :children disj child))))))


; TODO use mapv int :position ? clearer..
(def get-tile (comp int-posi :position))
