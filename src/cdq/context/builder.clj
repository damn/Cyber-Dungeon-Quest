(ns cdq.context.builder
  (:require [x.x :refer [defcomponent]]
            gdl.context
            [cdq.context :refer [get-property]]
            [cdq.entity :as entity]))

(defmethod cdq.context/transact! :tx/sound [[_ file] ctx]
  (gdl.context/play-sound! ctx file)
  nil)

(defn- create-creature-data [{:keys [property/id
                                     property/animation
                                     creature/flying?
                                     creature/faction
                                     creature/speed
                                     creature/hp
                                     creature/mana
                                     creature/skills
                                     creature/items
                                     creature/reaction-time]
                              [width height] :property/dimensions}
                             extra-components
                             context]
  (merge #:entity {:animation animation
                   :body {:width width :height height :solid? true}
                   :movement speed
                   :hp hp
                   :mana mana
                   :skills (zipmap skills (map #(get-property context %) skills)) ; TODO just set of skills use?
                   :inventory items
                   :flying? flying?
                   :faction faction
                   :z-order (if flying? :z-order/flying :z-order/ground)
                   :reaction-time reaction-time}
         extra-components
         (when (= id :creatures/lady-a) {:entity/clickable {:type :clickable/princess}})))

(defmethod cdq.context/transact! :tx/creature [[_ creature-id position extra-components] ctx]
  [[:tx/create (-> ctx
                   (get-property creature-id)
                   (create-creature-data extra-components ctx)
                   (assoc :entity/position position))]])

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
                property/animation]} (get-property ctx id)]
    [[:tx/sound sound]
     [:tx/create #:entity {:position position
                           :animation animation
                           :z-order :z-order/effect
                           :delete-after-animation-stopped? true}]]))
