(ns game.entities.creature
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.image :as image]
            [gdl.graphics.animation :as animation]
            [utils.core :refer [safe-get]]
            [game.context :as gm]
            [game.entity :as entity]
            [game.components.body :refer (assoc-left-bottom valid-position?)]
            [game.entities.audiovisual :as audiovisual]
            [game.player.entity :refer (set-player-entity)]))

(defn- create-images [context creature-name]
  (map #(image/create context
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
    (set-player-entity entity)
    (camera/set-position! world-camera (:position @entity)))
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
                             {:keys [is-player] :as extra-params}
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
    (if (valid-position? entity*)
      (gm/create-entity! context entity*)
      (println "Not able to spawn" creature-id "at" position))))
