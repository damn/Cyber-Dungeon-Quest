(ns game.ui.entity-info-window
  (:require [gdl.scene2d.ui :as ui]
            [gdl.scene2d.actor :as actor]
            [game.ui.mouseover-entity :refer (get-mouseover-entity)]))

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
       ; TODO make tree, scrollable in window possible ?
       ; debug windows mark
       ; not always mouseover? can type in id or select entity ?
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
                 :modifiers ; TODO
                 :items
                 ])
               :skills
               (keys (:skills @entity))))))))

(defn create []
  (let [window (ui/window :title "Info"
                          :id :entity-info-window)
        label (ui/label (entity-info-text))]
    (.expand (.add window label))
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.add window (actor/create :act (fn [_]
                                      (ui/set-text label (entity-info-text))
                                      (.pack window))))
    window))
