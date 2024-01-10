(ns cdq.context.builder
  (:require [x.x :refer [defcomponent]]
            gdl.context
            [cdq.context :refer [transact! get-property]]
            [cdq.entity :as entity]))

; * entity/body props => :width :height :solid? (also rotation angle, hmm)
; * => property/entity or something -> make also remove/add components
; => z-order ?! makes the constructor ?

(defn- create-creature-data [{:keys [entity/animation
                                     entity/flying?
                                     entity/faction
                                     entity/movement
                                     entity/hp
                                     entity/mana
                                     entity/skills
                                     entity/inventory
                                     entity/reaction-time]
                              [width height] :entity/body}]
  #:entity {:animation animation
            :body {:width width :height height :solid? true}
            :movement movement
            :hp hp
            :mana mana
            :skills skills
            :inventory inventory
            :flying? flying?
            :faction faction
            :z-order (if flying? :z-order/flying :z-order/ground)
            :reaction-time reaction-time})

(defmethod cdq.context/transact! :tx/creature [[_ creature-id extra-components] ctx]
  (let [entity-components (create-creature-data (get-property ctx creature-id))]
    [[:tx/create (merge entity-components
                        extra-components
                        (when (= creature-id :creatures/lady-a)
                          {:entity/clickable {:type :clickable/princess}}))]]))

(defcomponent :entity/plop _
  (entity/destroy [_ entity* ctx]
    [[:tx/audiovisual (:entity/position entity*) :projectile/hit-wall-effect]]))

; TODO if pass skills & creature props itself, function does not need context.
; properties themself could be the creature map even somehow
(extend-type gdl.context.Context
  cdq.context/Builder
  ; TODO use image w. shadows spritesheet
  (item-entity [_ position item]
    #:entity {:position position
              :body {:width 0.5 ; TODO use item-body-dimensions
                     :height 0.5
                     :solid? false}
              :z-order :z-order/on-ground
              :image (:property/image item)
              :item item
              :clickable {:type :clickable/item
                          :text (:property/pretty-name item)}})

  (line-entity [_ {:keys [start end duration color thick?]}]
    #:entity {:position start
              :z-order :z-order/effect
              :line-render {:thick? thick? :end end :color color}
              :delete-after-duration duration}))

(defmethod cdq.context/transact! :tx/audiovisual [[_ position id] ctx]
  (let [{:keys [property/sound
                entity/animation]} (get-property ctx id)]
    [[:tx/sound sound]
     [:tx/create #:entity {:position position
                           :animation animation
                           :z-order :z-order/effect
                           :delete-after-animation-stopped? true}]]))
