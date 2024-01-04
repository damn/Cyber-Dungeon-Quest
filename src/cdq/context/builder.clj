(ns cdq.context.builder
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [play-sound!]]
            [cdq.context :refer [create-entity! get-property]]
            [cdq.context.entity.body :refer (assoc-left-bottom)]))

(defn- create-creature-data [{:keys [property/id
                                     property/animation
                                     creature/flying?
                                     creature/faction
                                     creature/speed
                                     creature/hp
                                     creature/mana
                                     creature/skills
                                     creature/items]
                              [width height] :property/dimensions}
                             extra-components
                             context]
  (merge {:entity/animation animation
          :entity/body {:width width :height height :solid? true}
          :entity/movement speed
          :entity/hp hp
          :entity/mana mana
          :entity/skills (zipmap skills (map #(get-property context %) skills)) ; TODO just set of skills use?
          :entity/items items
          :entity/flying? flying?
          :entity/faction faction
          :entity/z-order (if flying? :z-order/flying :z-order/ground)}
         extra-components
         (when (= id :creatures/lady-a) {:entity/clickable {:type :clickable/princess}})))

(defcomponent :entity/plop _
  (cdq.context.ecs/destroy! [_ entity ctx]
    (cdq.context/audiovisual ctx (:entity/position @entity) :projectile/hit-wall-effect)))

; # LATER TODO
; * create! / destroy! check too
; * manual tick ...
; * TODO check all 25 functions working

; TODO replays should be working (with different speeds)
; => memory?
; multiplayer -> only relevant transactions (movement only no full entity uudate send?

; TODO send-event / state make namespaced
; even if I have to munge/demunge it always


; # HANDLER


; check instance? Entity ?
; 1. entity => reset! it ( needs its own atom somewhere )

; 2. nil => do nothing

; 3. vector of side effects (nil, entity, fn-calls)  =>



; 4. ctx/fns
; [:ctx/position-changed entity*]
; [:ctx/do-effect
; TODO merge ctx with effect ctx always
;  {:effect/source entity
;   :effect/target hit-entity}
;  hit-effect]
; [:ctx/send-event entity :alert]
; TODO check shout is working cons ..

; TODO maybe simpler always return seq of side effects?
; @ active skill anwyaway merged already effect-context?
; -> no need merge?




; 2. nil => do nothing
; 3. special fns:
; => namespaced! :side-effect/foo ?
; [:position-changed moved-entity*]
; [:do-effect effect-ctx effect]
; [:send-event entity :alert] ; TODO need context here ?!
; => no need to merge context w. effect-context
; _ we can do that for you  _
; see active skill tick
; start-action weird with merge contexts ...
; TODO check also giving one [:send-event]
; or is it part of a seqs of seqs??

; TODO ! passing vector or only map, ! check !

; side-effect/position-changed?
; (position-changed! ctx entity)
; (send-event! context entity :alert) ; / enter/exit/..
; @ create => try-pickup-item =>
; inventory only player? => pass inventory itself?

; =>>> maybe cell-grids, etc. also save entities itself
; not atoms ?? no need to deref??

#_(defn doseq-entity [e f & args]
  (doseq [k (keys @entity)]
    ; TODO what if k is not there anymore / v nil ?
    (let [entity* @entity]
      (tick [k (k entity*)] entity* ctx))

    )
  e)

; => the results of those functions again 'pure' ?
; => next step do-effect!

; # ETC:
; * flag destroyed, etc :entity/foo accessors put into protocol fns , lookups

(extend-type gdl.context.Context
  cdq.context/Builder
  (creature-entity [context creature-id position extra-components]
    (create-entity! context
                    (-> context
                        (get-property creature-id)
                        (create-creature-data extra-components context)
                        (assoc :entity/position position)
                        assoc-left-bottom)))

  (audiovisual [context position id]
    (let [{:keys [property/sound
                  property/animation]} (get-property context id)]
      (play-sound! context sound)
      (create-entity! context
                      {:entity/position position
                       :entity/animation animation
                       :entity/z-order :z-order/effect
                       :entity/delete-after-animation-stopped? true})))

  ; TODO use image w. shadows spritesheet
  (item-entity [context position item]
    (create-entity! context
                    {:entity/position position
                     :entity/body {:width 0.5 ; TODO use item-body-dimensions
                                   :height 0.5
                                   :solid? false}
                     :entity/z-order :z-order/on-ground
                     :entity/image (:property/image item)
                     :entity/item item
                     :entity/clickable {:type :clickable/item
                                        :text (:property/pretty-name item)}}))

  (line-entity [context {:keys [start end duration color thick?]}]
    (create-entity! context
                    {:entity/position start
                     :entity/z-order :z-order/effect
                     :entity/line-render {:thick? thick? :end end :color color}
                     :entity/delete-after-duration duration})))
