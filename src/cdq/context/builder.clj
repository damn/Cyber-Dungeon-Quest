(ns cdq.context.builder
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [play-sound!]]
            [gdl.graphics.animation :as animation]
            [gdl.math.vector :as v]
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
                     :entity/delete-after-duration duration}))

  ; TODO maxrange ?
  ; TODO make only common fields here
  (projectile-entity [context
                      {:keys [position faction size animation movement-vector hit-effect speed maxtime piercing]}]
    (create-entity! context
                    {:entity/position position
                     :entity/faction faction
                     :entity/body {:width size
                                   :height size
                                   :solid? false
                                   :rotation-angle (v/get-angle-from-vector movement-vector)
                                   :rotate-in-movement-direction? true}
                     :entity/flying? true
                     :entity/z-order :z-order/effect
                     :entity/movement speed
                     :entity/movement-vector movement-vector
                     :entity/animation animation
                     :entity/delete-after-duration maxtime
                     :entity/plop true
                     :entity/projectile-collision {:piercing piercing
                                                   :hit-effect hit-effect
                                                   :already-hit-bodies #{}}})))
