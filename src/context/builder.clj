(ns context.builder
  (:require [x.x :refer [defcomponent]]
            [reduce-fsm :as fsm]
            [gdl.context :refer [play-sound!]]
            [gdl.graphics.animation :as animation]
            [gdl.math.vector :as v]
            [cdq.context :refer [create-entity! get-property]]
            [context.entity.body :refer (assoc-left-bottom)]
            (context.entity.state
             [active-skill :as active-skill]
             [npc-dead :as npc-dead]
             [npc-idle :as npc-idle]
             [npc-sleeping :as npc-sleeping]
             [player-dead :as player-dead]
             [player-found-princess :as player-found-princess]
             [player-idle :as player-idle]
             [player-item-on-cursor :as player-item-on-cursor]
             [player-moving :as player-moving]
             [stunned :as stunned])))

(fsm/defsm-inc ^:private npc-fsm
  [[:sleeping
    :kill -> :dead
    :stun -> :stunned
    :alert -> :idle]
   [:idle
    :kill -> :dead
    :stun -> :stunned
    :start-action -> :active-skill]
   [:active-skill
    :kill -> :dead
    :stun -> :stunned
    :action-done -> :idle]
   [:stunned
    :kill -> :dead
    :effect-wears-off -> :idle]
   [:dead]])

(fsm/defsm-inc ^:private player-fsm
  [[:idle
    :kill -> :dead
    :stun -> :stunned
    :start-action -> :active-skill
    :pickup-item -> :item-on-cursor
    :movement-input -> :moving
    :found-princess -> :princess-saved]
   [:moving
    :kill -> :dead
    :stun -> :stunned
    :no-movement-input -> :idle]
   [:active-skill
    :kill -> :dead
    :stun -> :stunned
    :action-done -> :idle]
   [:stunned
    :kill -> :dead
    :effect-wears-off -> :idle]
   [:item-on-cursor
    :kill -> :dead
    :stun -> :stunned
    :drop-item -> :idle
    :dropped-item -> :idle]
   [:princess-saved]
   [:dead]])

(def ^:private npc-state-constructors
  {:sleeping     (fn [_ctx e] (npc-sleeping/->NpcSleeping e))
   :idle         (fn [_ctx e] (npc-idle/->NpcIdle e))
   :active-skill active-skill/->CreateWithCounter
   :stunned      stunned/->CreateWithCounter
   :dead         (fn [_ctx e] (npc-dead/->NpcDead e))})

(def ^:private player-state-constructors
  {:item-on-cursor (fn [_ctx e item] (player-item-on-cursor/->PlayerItemOnCursor e item))
   :idle           (fn [_ctx e] (player-idle/->PlayerIdle e))
   :moving         (fn [_ctx e v] (player-moving/->PlayerMoving e v))
   :active-skill   active-skill/->CreateWithCounter
   :stunned        stunned/->CreateWithCounter
   :dead           (fn [_ctx e] (player-dead/->PlayerDead e))
   :princess-saved (fn [_ctx e] (player-found-princess/->PlayerFoundPrincess e))})

(defn- ->state [& {:keys [player? initial-state]}]
  {:initial-state (if player?
                    :idle ; for savegame not, also initial-state.
                    initial-state)
   :fsm (if player?
          player-fsm
          npc-fsm)
   :state-obj-constructors (if player?
                             player-state-constructors
                             npc-state-constructors)})

; TODO player settings also @ editor ?!
(def ^:private player-components
  {:entity/player? true
   :entity/free-skill-points 3
   :entity/clickable {:type :clickable/player}})

(def ^:private lady-components
  {:entity/clickable {:type :clickable/princess}})

; TODO just pass components ! ( & validate entity schema ?)
; => check schema by going through all entities ?

; optional fields @ creature editor ?
; flying
; skills
; items
; => can also add clickable/princess ?

; New fields editor
; faction ?

; TODO hardcoded :lady-a
(defn- create-creature-data [{:keys [property/id
                                     property/animation
                                     creature/flying?
                                     creature/faction
                                     creature/speed
                                     creature/hp
                                     creature/mana
                                     creature/skills
                                     creature/items]
                              [width height] :property/dimensions
                              :as creature-props}
                             {:keys [player?
                                     initial-state] :as extra-params}
                             context]
  (let [princess? (= id :creatures/lady-a)]
    (merge (cond
            player? player-components
            princess? lady-components)
           {:entity/animation animation
            :entity/body {:width width :height height :solid? true}
            :entity/movement speed
            :entity/hp hp
            :entity/mana mana
            :entity/skills (zipmap skills (map #(get-property context %) skills)) ; TODO just set of skills use?
            :entity/items items
            :entity/flying? flying?
            :entity/faction faction
            :entity/z-order (if flying? :z-order/flying :z-order/ground)}
           (cond
            princess? nil
            :else {:entity/state (->state :player? player? :initial-state initial-state)})
           extra-params)))

(defcomponent :entity/plop _
  (context.entity/destroy! [_ entity ctx]
    (cdq.context/audiovisual ctx (:entity/position @entity) :projectile/hit-wall-effect)))

(extend-type gdl.context.Context
  cdq.context/Builder
  (creature-entity [context creature-id position creature-params]
    (create-entity! context
                    (-> context
                        (get-property creature-id)
                        (create-creature-data creature-params context)
                        (assoc :entity/position position)
                        assoc-left-bottom)))

  (audiovisual [context position id]
    (let [{:keys [sound
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
