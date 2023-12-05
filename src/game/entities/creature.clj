(ns game.entities.creature
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.image :as image]
            [gdl.graphics.world :as world]
            [gdl.graphics.animation :as animation]
            [game.db :as db]
            [game.entity :as entity]
            [game.properties :as properties]
            [game.utils.random :as rand]
            [game.components.body :refer (assoc-left-bottom valid-position?)]
            [game.entities.audiovisual :as audiovisual]
            [game.player.entity :refer (set-player-entity player-entity)]))

(defn- create-images [creature-name]
  (map #(image/create
         (str "creatures/animations/" creature-name "-" % ".png"))
       (range 1 5)))

(defn- images->world-unit-dimensions [images]
  (let [dimensions (map image/world-unit-dimensions images)
        max-width  (apply max (map first  dimensions))
        max-height (apply max (map second dimensions))]
    [max-width
     max-height]))

(defcomponent :is-player _
  (entity/create! [_ entity]
    (set-player-entity entity)
    (world/set-camera-position! (:position @entity)))
  (entity/tick! [_ entity delta]
    (world/set-camera-position! (:position @entity))))

(def ^:private player-components
  {:is-player true ; TODO is set @ map load!
   ; TODO death animation
   :faction :evil ; TODO set factions for creatures @ creatures.edn
   :mana 100 ; ??? if is-player true ?! or base mana for each creature ?!
   ; These all make player? true/false instead :npc/etc.
   ; or :npc/:player
   :choose-skill-type :player ; TODO use here also is-player directly ?
   :player-movement true      ; TODO use here also is-player directly ?
   :render-on-minimap true ; -> is player true
   :free-skill-points 3})

(def ^:private npc-components
  {:faction :good ; TODO set factions for creatures @ creatures.edn
   :default-monster-death true
   :choose-skill-type :npc
   :move-towards-enemy true}) ; -> different movement types as keywords & in entity-editor.

(defn- species-properties [species-id]
  (let [properties (properties/get species-id)
        multiplier {:id :species/multiplier, ; TODO
                    :speed 3,
                    :hp 10}]
    (-> properties
        (update :speed * (:speed multiplier))
        (update :hp #(int (* % (:hp multiplier)))))))

; TODO animation -> sub-component -> create from images,frame-duration,looping?
; creates itself / serializes / deserializes ....
; image too ... edn desrialization properties == components ??

; => actually-create-creature data is
; deserializing the entitz properties ? hmm not really
; but creating entities of a 'type'
; entity-systems for defentity ?

(defn- create-creature-data [properties {:keys [is-player] :as extra-params}]
  (let [creature-name (name (:id properties))
        properties (dissoc properties :id) ; create-entity adds id
        properties (update properties :skills #(or % []))
        images (create-images creature-name)
        [width height] (images->world-unit-dimensions images)
        ; TODO merge these speed/hp just into properties
        {:keys [speed hp]} (species-properties (:species properties))]
    ; (distinct (mapcat keys (vals creatures)))
    ; (:image :id :creature-type :items :level :skills)
    ; :image -> gets rendered in game => do not merge !
    (merge (dissoc properties :image)
           (if is-player ; ideally remove this -> entity-editor ability to add fields to specific entities (free-skill-points 3)
             player-components
             npc-components)
           {:body {:width width
                   :height height
                   :is-solid true} ; parameter ? how do non-solid entities move ? (:collides?)  ?
            :speed speed
            :hp hp
            :mana 11
            :is-flying false ; TODO not used yet @ movement code (flying?) -> grep for 'is-' ...
            :animation (animation/create images :frame-duration 250 :looping true)
            :z-order (if (:is-flying properties) ; not existing, :flying?
                       :flying
                       :ground)}
           extra-params)))

(def ^:private monster-drop-table
  {{"Battle-Drugs" 1} 1
   {"Mana-Potion" 3
    "Heal-Potion" 3
    ; wrong color black/white
    ;"Big-Mana-Potion" 1
    ;"Big-Heal-Potion" 1
    } 10})

(defcomponent :default-monster-death _
  (entity/destroy! [_ entity]
    (audiovisual/create! (:position @entity)
                         :creature/die-effect)))

; TODO do n ot assoc left-bottom and position
; -> create entity and afterwards check if valid-position? of the entity !
; or is it asserted invalid position ?
; or try/catch error :invalid-position
(defn create! [creature-id position creature-params]
  ; calls create-entity! just for creature-id entity
  ; and checks for invalid-position error
  (let [entity* (-> creature-id
                    properties/get
                    (create-creature-data creature-params)
                    (assoc :position position)
                    assoc-left-bottom)] ; TODO strange that it needs that , make case if left-bottom not available?
    ; => do this @ body & error if create-entity fails @ body-component
    ; or spawn @ left-bottom
    ; -> use mostly left-bottom & width / height and use less center-position and half-width / half-height
    (if (valid-position? entity*)
      (db/create-entity! entity*)
      (println "Not able to spawn" creature-id "at" position))))

; TODO
; * remove check for valid position
; * throw error invalid-position-error on create and handle it ?
; but not put in some cells/contenfields already?

; maybe
; 1. construct components properties (without side effects)
; 2. create-entity! adds to cell-grid/etc.
