(ns context.builder
  (:require [reduce-fsm :as fsm]
            [gdl.context :refer [create-image play-sound!]]
            [gdl.graphics.animation :as animation]
            [game.context :refer [create-entity! get-property]]
            [game.components.body :refer (assoc-left-bottom)]
            (game.components.state
             [active-skill :as active-skill]
             [npc-dead :as npc-dead]
             [npc-idle :as npc-idle]
             [npc-sleeping :as npc-sleeping]
             [player-dead :as player-dead]
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
    :movement-input -> :moving]
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
   [:dead]])

(def ^:private npc-state-constructors
  {:sleeping     npc-sleeping/->State
   :idle         npc-idle/->State
   :active-skill active-skill/->CreateWithCounter
   :stunned      stunned/->CreateWithCounter
   :dead         npc-dead/->State})

(def ^:private player-state-constructors
  {:item-on-cursor player-item-on-cursor/->State
   :idle           player-idle/->State
   :moving         player-moving/->State
   :active-skill   active-skill/->CreateWithCounter
   :stunned        stunned/->CreateWithCounter
   :dead           player-dead/->State})

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
   :faction :evil
   :mana 100
   :choose-skill-type :player
   :player-movement true
   :render-on-minimap true
   :free-skill-points 3})

(def ^:private npc-components
  {:faction :good
   :default-monster-death true
   :choose-skill-type :npc
   :move-towards-enemy true})

(defn- species-properties [species-props]
  (let [multiplier {:id :species/multiplier,
                    :speed 3,
                    :hp 10}]
    (-> species-props
        (update :speed * (:speed multiplier))
        (update :hp #(int (* % (:hp multiplier)))))))

(defn- create-creature-data [creature-props
                             {:keys [is-player
                                     initial-state] :as extra-params}
                             context]
  (let [creature-name (name (:id creature-props))
        creature-props (dissoc creature-props :id)
        creature-props (update creature-props :skills #(or % []))
        images (create-images context creature-name)
        [width height] (images->world-unit-dimensions images)
        {:keys [speed hp]} (species-properties (get-property context (:species creature-props)))]
    (merge (dissoc creature-props :image)
           (if is-player
             player-components
             npc-components)
           {:body {:width width
                   :height height
                   :is-solid true}
            :entity/movement speed
            :hp hp
            :mana 11
            :is-flying false
            :animation (animation/create images :frame-duration 250 :looping? true)
            :entity/state {:initial-state (if is-player
                                            :idle
                                            initial-state)
                           :fsm (if is-player
                                  player-fsm
                                  npc-fsm)
                           :state-obj-constructors (if is-player
                                                     player-state-constructors
                                                     npc-state-constructors)}
            :z-order (if (:is-flying creature-props)
                       :flying
                       :ground)}
           extra-params)))



(extend-type gdl.context.Context
  game.context/Builder
  (creature-entity [context creature-id position creature-params]
    (let [entity* (-> context
                      (get-property creature-id)
                      (create-creature-data creature-params context)
                      (assoc :position position)
                      assoc-left-bottom)]
      (create-entity! context entity*)))

  (audiovisual [context position id]
    (let [{:keys [sound animation]} (get-property context id)]
      (play-sound! context sound)
      (create-entity! context
                      {:position position
                       :animation animation
                       :z-order :effect
                       :delete-after-animation-stopped? true})))


  ; TODO use image w. shadows spritesheet
  (item-entity [context position item]
    (create-entity! context
                    {:position position
                     :body {:width 0.5 ; TODO use item-body-dimensions
                            :height 0.5
                            :is-solid false} ; solid? collides?
                     :z-order :on-ground
                     :image (:image item)
                     :item item
                     ; :mouseover-text (:pretty-name item)
                     ; :clickable :item
                     :clickable {:type :item; TODO item-color also from text...? uniques/magic/... TODO namespaced keyword.
                                 :text (:pretty-name item)}}))

  (line-entity [context {:keys [start end duration color thick?]}]
    (create-entity! context
                    {:position start
                     :z-order :effect
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
                     :faction faction
                     :body {:width size
                            :height size
                            :is-solid false
                            :rotation-angle 0
                            :rotate-in-movement-direction? true}
                     ; TODO forgot to add :is-flying true !!!
                     ; blocked by stones which I can see over
                     :z-order :effect
                     :entity/movement speed
                     :movement-vector movement-vector
                     :animation animation
                     :delete-after-duration maxtime
                     :projectile-collision {:piercing piercing
                                            :hit-effect hit-effect
                                            :already-hit-bodies #{}}})))
