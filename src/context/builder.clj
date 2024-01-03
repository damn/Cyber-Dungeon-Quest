(ns context.builder
  (:require [x.x :refer [defcomponent]]
            [reduce-fsm :as fsm]
            [gdl.context :refer [create-image play-sound!]]
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

(defn- ->state [& {:keys [is-player initial-state]}]
  {:initial-state (if is-player
                    :idle
                    initial-state)
   :fsm (if is-player
          player-fsm
          npc-fsm)
   :state-obj-constructors (if is-player
                             player-state-constructors
                             npc-state-constructors)})

(defn- create-images [context creature-name]
  (map #(create-image context
                      (str "creatures/animations/" creature-name "-" % ".png"))
       (range 1 5)))

(defn- images->world-unit-dimensions [images]
  (let [dimensions (map :world-unit-dimensions images)
        max-width  (apply max (map first  dimensions))
        max-height (apply max (map second dimensions))]
    [max-width
     max-height]))

(def ^:private player-components
  {:is-player true
   :entity/faction :evil
   ;:entity/mana 100 ; is overwritten
   :player-movement true
   :free-skill-points 3
   :entity/clickable {:type :clickable/player}})

(def ^:private npc-components
  {:entity/faction :good
   :move-towards-enemy true})

(def ^:private lady-props
  {:entity/faction :good
   :entity/clickable {:type :clickable/princess}})

(defn- species-properties [species-props]
  (let [multiplier {:id :species/multiplier, :speed 2, :hp 10}]
    (-> species-props
        (update :creature/speed * (:speed multiplier))
        (update :creature/hp #(int (* % (:hp multiplier)))))))

; TODO hardcoded :lady-a
(defn- create-creature-data [creature-props
                             {:keys [is-player
                                     initial-state] :as extra-params}
                             context]
  (let [creature-id (:property/id creature-props)
        creature-props (update creature-props :creature/skills #(or % []))
        images (create-images context (name creature-id))
        [width height] (images->world-unit-dimensions images)
        {:keys [creature/speed
                creature/hp]} (species-properties (get-property context (:creature/species creature-props)))
        princess? (= creature-id :creatures/lady-a)]
    (merge (cond
            is-player player-components
            princess? lady-props
            :else     npc-components)
           {:entity/body {:width width
                          :height height
                          :is-solid true}
            :entity/movement speed
            :entity/hp hp
            :entity/mana 11
            :entity/skills (:creature/skills creature-props)
            :entity/items  (:creature/items creature-props)
            :entity/is-flying false
            :entity/animation (animation/create images :frame-duration 0.1 :looping? true)
            :entity/z-order (if (:is-flying creature-props) :flying :ground)}
           (cond
            princess? nil
            :else {:entity/state (->state :is-player is-player
                                          :initial-state initial-state)})
           extra-params)))

(defcomponent :entity/plop _
  (context.entity/destroy! [_ entity ctx]
    (cdq.context/audiovisual ctx (:position @entity) :projectile/hit-wall-effect)))

(extend-type gdl.context.Context
  cdq.context/Builder
  (creature-entity [context creature-id position creature-params]
    (create-entity! context
                    (-> context
                        (get-property creature-id)
                        (create-creature-data creature-params context)
                        (assoc :position position)
                        assoc-left-bottom)))

  (audiovisual [context position id]
    (let [{:keys [sound animation]} (get-property context id)]
      (play-sound! context sound)
      (create-entity! context
                      {:position position
                       :entity/animation animation
                       :entity/z-order :effect
                       :delete-after-animation-stopped? true})))

  ; TODO use image w. shadows spritesheet
  (item-entity [context position item]
    (create-entity! context
                    {:position position
                     :entity/body {:width 0.5 ; TODO use item-body-dimensions
                                   :height 0.5
                                   :is-solid false}
                     :entity/z-order :on-ground
                     :image (:property/image item)
                     :item item
                     :entity/clickable {:type :clickable/item
                                        :text (:property/pretty-name item)}}))

  (line-entity [context {:keys [start end duration color thick?]}]
    (create-entity! context
                    {:position start
                     :entity/z-order :effect
                     :line-render {:thick? thick?
                                   :end end
                                   :color color}
                     :delete-after-duration duration}))

  ; TODO maxrange ?
  ; TODO make only common fields here
  (projectile-entity [context
                      {:keys [position faction size animation movement-vector hit-effect speed maxtime piercing]}]
    (create-entity! context
                    {:position position
                     :entity/faction faction
                     :entity/body {:width size
                                   :height size
                                   :is-solid false
                                   :rotation-angle (v/get-angle-from-vector movement-vector)
                                   :rotate-in-movement-direction? true}
                     ; TODO forgot to add :is-flying true !!!
                     ; blocked by stones which I can see over
                     :entity/z-order :effect
                     :entity/movement speed
                     :entity/movement-vector movement-vector
                     :entity/animation animation
                     :delete-after-duration maxtime
                     :entity/plop true
                     :projectile-collision {:piercing piercing
                                            :hit-effect hit-effect
                                            :already-hit-bodies #{}}})))
