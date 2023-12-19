(ns game.entities.creature
  (:require [reduce-fsm :as fsm]
            [x.x :refer [defcomponent]]
            [gdl.graphics.camera :as camera]
            [gdl.context :refer [create-image]]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get]]
            [game.context :as gm]
            [game.entity :as entity]
            [game.components.body :refer (assoc-left-bottom valid-position?)]
            (game.components.state
             [active-skill :as active-skill]
             [npc-dead :as npc-dead]
             [npc-idle :as npc-idle]
             [npc-sleeping :as npc-sleeping]
             [player-dead :as player-dead]
             [player-idle :as player-idle]
             [player-item-on-cursor :as player-item-on-cursor]
             [player-moving :as player-moving]
             [stunned :as stunned])
            [game.entities.audiovisual :as audiovisual]))

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

(defcomponent :is-player _
  (entity/create! [_ entity {:keys [world-camera]}]
    (camera/set-position! world-camera (:position @entity)))
  ; TODO make on position changed trigger
  (entity/tick! [_ {:keys [world-camera]} entity delta]
    (camera/set-position! world-camera (:position @entity))))

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
                             {:keys [context/properties] :as context}]
  (let [creature-name (name (:id creature-props))
        creature-props (dissoc creature-props :id)
        creature-props (update creature-props :skills #(or % []))
        images (create-images context creature-name)
        [width height] (images->world-unit-dimensions images)
        {:keys [speed hp]} (species-properties (safe-get properties (:species creature-props)))]
    (merge (dissoc creature-props :image)
           (if is-player
             player-components
             npc-components)
           {:body {:width width
                   :height height
                   :is-solid true}
            :speed speed
            :hp hp
            :mana 11
            :is-flying false
            :animation (animation/create images :frame-duration 250 :looping? true)
            :components/state {:initial-state (if is-player
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

(defcomponent :default-monster-death _
  (entity/destroy! [_ entity context]
    (audiovisual/create! context
                         (:position @entity)
                         :creature/die-effect)))

(defn create! [creature-id position creature-params {:keys [context/properties] :as context}]
  (let [entity* (-> (safe-get properties creature-id)
                    (create-creature-data creature-params context)
                    (assoc :position position)
                    assoc-left-bottom)]
    (if (valid-position? context entity*)
      (gm/create-entity! context entity*)
      (do
       (println "Not able to spawn" creature-id "at" position)
       (throw (Error. (str "Not able to spawn " creature-id " at " position)))
       ))))
