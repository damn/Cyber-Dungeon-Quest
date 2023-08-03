(nsx game.items.core
  (:require [game.properties :as properties]
            [game.skills.core :as skills]
            [game.player.entity :refer (player-entity)]))

; TODO
; * shield-wood-buckler / rings not having modifiers anymore
; * center sprites (staff not centered for example)
; * :name used as :id => (look at this later, like skill-type)

(defn- pretty-name [id]
  (->> (str/split (name id) #"-")
       (map str/capitalize)
       reverse
       (str/join " ")))

; :modifiers [[:shield [:target :physical 0.2]]]
;-    <property name="modifiers" value="[[:shield [:target :physical 0.2]]]"/>
;-    <property name="name" value="shield-wood-buckler"/>
;
;-    <property name="modifiers" value="[[:damage [:source :physical [:val :inc] 5]]]"/>
;-    <property name="name" value="ring-gold"/>
;
;-    <property name="modifiers" value="[[:damage [:source :magic [:max :mult] 0.5]]]"/>
;-    <property name="name" value="ring-azure"/>

(defn- prepare-item-properties [{:keys [id modifiers] :as properties}]
  (assoc properties
         :name id
         :pretty-name (pretty-name id)))

; TODO
; {:id :weapon/defaults, :range 0.5, :action-time 500, :damage [5 10]}

(defn- apply-weapon-defaults [{:keys [damage] :as properties}]
  (-> properties
      (update :action-time #(int (* 500 %)))
      (assoc :maxrange (* 0.5 (:range properties)))
      (assoc :damage (mapv #(int (* % damage)) [5 10]))))

(defn- add-weapon-skill-effect [{:keys [maxrange damage] :as properties}]
  (assoc properties :effect
         [:target-entity {:maxrange maxrange
                          :hit-effects [[:damage [:physical damage]]]}]))

(defn- handle-weapons [{:keys [id slot] :as properties}]
  (if (and (= :weapon slot)
           (:action-time properties)) ; nil when not implemented yet.
    (let [weapon (-> properties
                     (update :modifiers conj [:skill id])
                     apply-weapon-defaults ; this function move here just like this, no extra fn.
                     add-weapon-skill-effect)] ; this same.
      (alter-var-root #'skills/skills assoc id weapon)
      weapon)
    properties))

; WEAPONS DO NOT HAVE SKILL MODIFIER - special case
; they ARE the skill ?? or both ?

; actually we have only 1 'ATTACK' skill which
; checks what 'weapon' you are wearing ....

; => attack parameters ...

; ; this function move here just like this, no extra fn.
; WOW ! TODO FIXME ! functions which are just used once do not make into functions !
; maximum simple !!


; TODO weapons need to be their own thing somehow
; no 'if'

; no skill modifier -> special case @ equip uneqip (also special entity description)
; also skills properties & items properties in same 'properties' ( <= / 'entities' ?! )
; map or DB
; then also description text for weapons has be default these stats, entity-description stats ?!
;

(app/defmanaged items (properties/load-edn "items/properties.edn"
                                           :transform
                                           (comp handle-weapons
                                                 prepare-item-properties)))

; diablo2 unique gold rgb 144 136 88
(color/defrgb ^:private gold-item-color 0.84 0.8 0.52)
; diablo2 blue magic rgb 72 80 184
(color/defrgb modifiers-text-color 0.38 0.47 1)

; TODO write 'two handed' at weapon info -> key-to-pretty-tooltip-text function for keywords (extend-c?)
; TODO showing 'Sword' -> 'Sword' modifier -> stupid

(defn- item-name [item]
  (str (:pretty-name item)
       (when-let [cnt (:count item)]
         (str " (" cnt ")"))))

(defn text [item]
  (str (if (= (:slot item) :weapon)
         "" ; already in first modifier name of weapon skill == item name (also pretty-name?!)
         (str (item-name item) "\n"))
       ; TODO not player-entity but referenced entity, TODO only used @ skill modifier
       ; no need for this here ?
       (str/join "\n"
                 (for [modifier (:modifiers item)]
                   (modifiers/text player-entity modifier)))))
