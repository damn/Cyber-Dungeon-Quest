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
                         :creature/die-effect)
    #_(let [position (:position @entity)]
        (rand/if-chance 20
                        (let [item-name (-> monster-drop-table
                                            rand/get-rand-weighted-item
                                            rand/get-rand-weighted-item)]
                          ; TODO pass item instance
                          (item-entity/create! position item-name)))
        (rand/if-chance 25
                        (create-rand-item position :max-lvl (:level @entity))))))

; TODO use defrecord pass
; can be done automatically @ x.db ?
#_(defrecord Entity [id
                     is-player
                     position

                     ; body:
                     width
                     height
                     half-width
                     half-height
                     is-solid

                     speed

                     hp
                     mana
                     z-order])


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


; groups as of level/spawn-chance/spawn-groups =>
; => lvls can also spawn together ?
; group lvl = 1
; spawns 1 lvl 1
; group lvl 3
; 1 lvl 3 or 1 lvl2 1 lvl3 or 3 lvl1 !
; more variable group size 0-30?
; group-type (goblins, etc.) => spawn together
; group-idx
; chance-spawn idx (lower less)

; function is for monsters with width&height <1 tile
(comment

 (defn spawn-group [group posis & {maxno :maxno :or {maxno 9}}]
  (let [posis (map translate-to-tile-middle (take maxno posis))
        ;; dont spawn monsters near the start locations of the levels ... so a calm start... start-positions need to be translated to tile middle !!
        start-position (:start-position (get-current-map-data))
        posis (remove #(< (v/distance % start-position)
                          game.components.sleeping/aggro-range)
                      posis)
        monstertypes (get-rand-weighted-items (count posis) group)]
    (comment (log "#posis " (count posis)
                  " group: " group
                  " frequencies: " (frequencies monstertypes)))
    (doall ; returning it
      (remove nil?
              (map #(try-spawn %1 %2) posis monstertypes)))))

(use '[mapgen.spawn-spaces :only (get-spawn-positions-groups)])
(use '[game.maps.cell-grid :only (get-cell-grid)])

(defn spawn-monsters [group-definitions]
  (doseq [posis (get-spawn-positions-groups (get-cell-grid))]
    (apply spawn-group
           (rand-nth group-definitions)
           posis
           (apply concat more)))))
