(ns cdq.context.builder
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [play-sound!]]
            [cdq.context :refer [create-entity! get-property set-cursor! show-msg-to-player!]]
            [cdq.entity :as entity]
            [cdq.entity.body :refer (assoc-left-bottom)]))

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
  (merge #:entity {:animation animation
                   :body {:width width :height height :solid? true}
                   :movement speed
                   :hp hp
                   :mana mana
                   :skills (zipmap skills (map #(get-property context %) skills)) ; TODO just set of skills use?
                   :items items
                   :flying? flying?
                   :faction faction
                   :z-order (if flying? :z-order/flying :z-order/ground)}
         extra-components
         (when (= id :creatures/lady-a) {:entity/clickable {:type :clickable/princess}})))

(defcomponent :entity/plop _
  (entity/destroy [_ entity* ctx]
    [[:tx/audiovisual (:entity/position entity*) :projectile/hit-wall-effect]]))

; TODO if pass skills & creature props itself, function does not need context.
; properties themself could be the creature map even somehow
(extend-type gdl.context.Context
  cdq.context/Builder
  (creature [context creature-id position extra-components]
    (-> context
        (get-property creature-id)
        (create-creature-data extra-components context)
        (assoc :entity/position position)
        assoc-left-bottom))

  ; TODO probably separate in 2 txs
  (audiovisual [context position id]
    (let [{:keys [property/sound
                  property/animation]} (get-property context id)]
      (play-sound! context sound)
      (create-entity! context
                      #:entity {:position position
                                :animation animation
                                :z-order :z-order/effect
                                :delete-after-animation-stopped? true})))

  ; TODO use image w. shadows spritesheet
  (item-entity [context position item]
    (create-entity! context
                    #:entity {:position position
                              :body {:width 0.5 ; TODO use item-body-dimensions
                                            :height 0.5
                                            :solid? false}
                              :z-order :z-order/on-ground
                              :image (:property/image item)
                              :item item
                              :clickable {:type :clickable/item
                                                 :text (:property/pretty-name item)}}))

  (line-entity [_ {:keys [start end duration color thick?]}]
    #:entity {:position start
              :z-order :z-order/effect
              :line-render {:thick? thick? :end end :color color}
              :delete-after-duration duration}))

(defmethod cdq.context/transact! :tx/sound [[_ file] ctx]
  (play-sound! ctx file))

(defmethod cdq.context/transact! :tx/audiovisual [[_ position id] ctx]
  (cdq.context/audiovisual ctx position id))

(defmethod cdq.context/transact! :tx/cursor [[_ cursor-key] ctx]
  (set-cursor! ctx cursor-key))

(defmethod cdq.context/transact! :tx/msg-to-player [[_ message] ctx]
  (show-msg-to-player! ctx message))
