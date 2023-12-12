(ns game.entities.creature
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.image :as image]
            [gdl.graphics.animation :as animation]
            [game.db :as db]
            [game.entity :as entity]
            [game.properties :as properties]
            [game.components.body :refer (assoc-left-bottom valid-position?)]
            [game.entities.audiovisual :as audiovisual]
            [game.player.entity :refer (set-player-entity)]))

(defn- create-images [context creature-name]
  (map #(image/create context
                      (str "creatures/animations/" creature-name "-" % ".png"))
       (range 1 5)))

(defn- images->world-unit-dimensions [images]
  (let [dimensions (map image/world-unit-dimensions images)
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

(defn- species-properties [species-id]
  (let [properties (properties/get species-id)
        multiplier {:id :species/multiplier,
                    :speed 3,
                    :hp 10}]
    (-> properties
        (update :speed * (:speed multiplier))
        (update :hp #(int (* % (:hp multiplier)))))))

(defn- create-creature-data [properties {:keys [is-player] :as extra-params} context]
  (let [creature-name (name (:id properties))
        properties (dissoc properties :id)
        properties (update properties :skills #(or % []))
        images (create-images context creature-name)
        [width height] (images->world-unit-dimensions images)
        {:keys [speed hp]} (species-properties (:species properties))]
    (merge (dissoc properties :image)
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
            :z-order (if (:is-flying properties)
                       :flying
                       :ground)}
           extra-params)))

(defcomponent :default-monster-death _
  (entity/destroy! [_ entity context]
    (audiovisual/create! context
                         (:position @entity)
                         :creature/die-effect)))

(defn create! [creature-id position creature-params context]
  (let [entity* (-> creature-id
                    properties/get
                    (create-creature-data creature-params context)
                    (assoc :position position)
                    assoc-left-bottom)]
    (if (valid-position? entity*)
      (db/create-entity! entity* context)
      (println "Not able to spawn" creature-id "at" position))))
