(nsx game.ui.entity-info
  (:require [game.ui.mouseover-entity :refer (get-mouseover-entity)]))

; I can create for every entity
; skill-icons with tooltips
; item-icons
; effect-modifiers ???
; full menu?
; thats crazy...

(defn- entity-info-text []
  (when-let [entity (get-mouseover-entity)]
    ; (game.creatures.core/text entity) ?
    ; wait! its for all entities (projectiles, items, etc.)
    ; => debug ?
    ; => or for each entity type implement multimethod
    ; => but the frame blocks view , needs to be verschiebable
    ; => entity-type == create-entity! used !
    ; or can be mouseover-ed...
    (binding [*print-level* nil]
      (with-out-str
       (clojure.pprint/pprint
        (assoc (select-keys
                @entity
                [:id
                 :name
                 :speed
                 :hp
                 :mana
                 :faction
                 :creature
                 :level
                 :is-flying
                 :active-skill?
                 :effect-modifiers
                 :items
                 ])
               :skills
               (keys (:skills @entity))))))))

(app/on-create
 (def window (ui/window "Info"))

 (def ^:private label (ui/label (entity-info-text)))
 (.expand (.add window label))

 ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
 ; => fix size somehow.
 (.add window (ui/actor
               #(do
                 (.setText label (entity-info-text))
                 (.pack window)
                 ))))
