; TODO 'effects'
(nsx game.entity.nova
  (:require [game.utils.counter :refer :all]
            [game.maps.cell-grid :as cell-grid])
  (:use clojure.set
        (game.components render)))

; TODO I can do this on a per-effect basis
; e.g. entity which does not have hitpoints (a spirit)
; but has :speed and I do slowdown -> better.
; -> only take what you need.
; -> slowdown -> slows everything which has speed
; -> damage -> everything with hitpoints
; -> mana eveyrthing with . mana (entities with mana but no speed/hitpoints ? -> you can steal mana !)
#_(defn get-destructible-bodies [circle faction]
  (let [affects-faction (if (keyword? faction) #{faction} (set faction))]
    (filter #(and (:hp @%)
                  (affects-faction (:faction @%)))
            ; TODO line of sight !? or not , depends on nova-magic properties.
      (cell-grid/circle->touched-entities circle))))

#_(defctypefn :render :nova [{{:keys [radius]} :nova} position]
  (draw-circle position radius white)
  ;(draw-circle position (/ radius 2) black)
  ;(draw-circle position (/ radius 4) white)
  ;(draw-circle position (/ radius 8) black)
  )

#_(defctypefn :db/update-entity! :nova
  [entity delta]
  (let [{:keys [position
                maxradius
                radius
                already-hit
                counter
                hit-effects
                affects-faction
                is-player-spell]} (:nova @entity)
        radius (* (ratio counter) maxradius) ; erst ratio danach update-counter, sonst beim letzten update => ratio=0
        hits (remove #(contains? already-hit %)
                     (get-destructible-bodies
                      {:position position :radius radius}
                      affects-faction))]
    (doseq [target hits]
      (effects/do-effects! {:source entity :target target} hit-effects))
    (update-in! entity [:nova]
                #(-> %
                     (assoc :radius radius)
                     (update-in [:already-hit] union (set hits))))
    (when (update-counter! entity delta [:nova :counter])
      (swap! entity assoc :destroyed? true))))

#_(defnks nova-effect [:position :duration :maxradius :affects-faction :opt :is-player-spell]
  (create-entity!
   ;(circle-debug-comp)
   {:position position
    ; because affects entities behind walls
    :z-order :effect
    ; TODO move into entity attributes / but then how to connect defctypefn
    ; -> maybe defctypefns should be namespaced keywords then we can just use :radius here ...
    :nova {:position position ; TODO use position component !? or doesnt matter
           :maxradius maxradius
           :radius 0
           :already-hit #{}
           :counter (make-counter duration) ; TODO move inside nova can you counter component.
           :hit-effects [[:damage [:magic [5 10]]]]
           :affects-faction affects-faction
           :is-player-spell is-player-spell}}))


#_(defmethod do-effect :nova [{:keys [radius dmg]} entity]
  (nova-effect
   :position (:position @entity)
   :duration 100
   :maxradius radius
   :affects-faction :monster
   :dmg dmg
   :is-player-spell true))

#_(defskill :nova
  {:cost 10
   :action-time 300
   :cooldown 0
   :is-spell true
   :icon (get-skill-icon :nova)
   :dmg [3 7]
   :radius 1.5 })
