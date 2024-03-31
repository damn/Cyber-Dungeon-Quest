(ns cdq.attributes
  (:require [malli.core :as m]
            [data.val-max :refer [val-max-schema]]))

; TODO new component/modifier/entity component -> add attribute here
; or add it to the defcomponent ?!

; TODO entity components, what subset is allowed?
; which components depend on which?
; which can not be removed?
; => removable/optional !
; => for creature npc state it needs skills, etc.
; => property/creature or creature/entity has different optional/required than other entities (stones)
; because for creature we are adding npc-state / !player-state!

; TODO namespaced attr all sub-attrs (hit-effect, etc. , body not is record ? or transform @ body create-component)

;;

(def attributes {})

(defn- defattribute [k data]
  ; TODO add :optional? key which is optional ....
  (assert (:schema data) k)
  (assert (:widget data) k)
  (alter-var-root #'attributes assoc k data))

;; attribute types

(def ^:private sound        {:widget :sound      :schema :string})
(def ^:priate  image        {:widget :image      :schema :some})
(def ^:private animation    {:widget :animation  :schema :some})
(def ^:private string-attr  {:widget :text-field :schema :string})
(def ^:private boolean-attr {:widget :check-box  :schema :boolean :default-value true})
(def ^:private val-max-attr {:widget :text-field :schema (m/form val-max-schema)})
(def ^:private nat-int-attr {:widget :text-field :schema nat-int?})
(def ^:private pos-attr     {:widget :text-field :schema pos?})
(def ^:private pos-int-attr {:widget :text-field :schema pos-int?})

(defn- enum [& items]
  {:widget :enum
   :schema (apply vector :enum items)
   :items items})

; TODO not checking if one of existing ids used
; widget requires property/image.
(defn- one-to-many-ids [property-type]
  {:widget :one-to-many
   :schema [:set :qualified-keyword]
   :linked-property-type property-type}) ; => fetch from schema namespaced ?

(defn- map-attribute [& attr-ks] ; TODO similar to components-attribute
  {:widget :nested-map
   :schema (vec (concat [:map {:closed true}]
                        (for [k attr-ks]
                          (vector k (:schema (get attributes k))))))})

(defn- components-attribute [component-namespace]
  (let [component-attributes (filter #(= (name component-namespace) (namespace %))
                                     (keys attributes))]
    {:widget :nested-map
     :schema (vec (concat [:map {:closed true}]
                          (for [k component-attributes]
                            [k {:optional true} (:schema (get attributes k))])))
     :components component-attributes})) ; => fetch from schema ? (optional? )

;;

; TODO this is == :optional key @ components-attribute ?
(defn removable-component? [k]
  (#{"tx" "modifier" #_"entity"} (namespace k)))

;;

(defattribute :property/image       image)
(defattribute :property/sound       sound)
(defattribute :property/pretty-name string-attr)

; TODO how 2 do default values,its not default-values , its non-optional attributes !
; similar to components nested-map
;:default-value {:width 0.5 :height 0.5 :solid? true}

; TODO label == not editable
(defattribute :width  {:widget :label :schema pos?}) ; TODO make px
(defattribute :height {:widget :label :schema pos?}) ; TODO make px
(defattribute :solid? {:widget :label :schema boolean?})

(defattribute :entity/body          (map-attribute :width :height :solid?)) ; TODO body assert >+ min body size?
(defattribute :entity/skills        (one-to-many-ids :property.type/skill)) ; required by npc state, also mana!, also movement (no not needed, doesnt do anything then)
(defattribute :entity/inventory     (one-to-many-ids :property.type/item)) ; optional
(defattribute :entity/animation     animation) ; optional
(defattribute :entity/mana          nat-int-attr) ; required @ npc state, for cost, check if nil
(defattribute :entity/flying?       boolean-attr) ; optional, mixed with z-order
(defattribute :entity/hp            pos-int-attr) ; required for target-entity (remove)
(defattribute :entity/movement      pos-attr) ; optional, only assoc'ing movement-vector
(defattribute :entity/reaction-time pos-attr)
(defattribute :entity/faction       (enum :good :evil))

(defattribute :property/entity (components-attribute :entity))

(defattribute :damage/type    (enum :physical :magic))
(defattribute :damage/min-max val-max-attr)

(defattribute :maxrange           pos-attr)

(defattribute :tx/damage          (map-attribute :damage/type :damage/min-max))
(defattribute :tx/sound           sound)
(defattribute :tx/spawn           {:widget :text-field :schema [:qualified-keyword {:namespace :creatures}]}) ; => one to one attr!
(defattribute :tx/stun            pos-attr)
(defattribute :tx/restore-hp-mana {:widget :text-field :schema [:= true]}) ; TODO no schema
(defattribute :tx/projectile      {:widget :text-field :schema [:= true]})
(defattribute :tx/target-entity   {:widget :nested-map ; TODO circular depdenency components-attribute  - cannot use map-attribute..
                                   :schema [:map {:closed true}
                                            [:hit-effect [:map]]
                                            [:maxrange pos?]]
                                   :default-value {:hit-effect {}
                                                   :max-range 2.0}})

(defattribute :skill/effect (components-attribute :tx))
(defattribute :hit-effect   (components-attribute :tx))

(defattribute :modifier/max-hp       {:widget :text-field :schema number?})
(defattribute :modifier/max-mana     {:widget :text-field :schema number?})
(defattribute :modifier/cast-speed   pos-attr)
(defattribute :modifier/attack-speed pos-attr)
(defattribute :modifier/shield       {:widget :text-field :schema :some}) ; TODO no schema
(defattribute :modifier/armor        {:widget :text-field :schema :some}) ; TODO no schema
(defattribute :modifier/damage       {:widget :text-field :schema :some}) ; TODO no schema

(defattribute :item/modifier (components-attribute :modifier))
(defattribute :item/slot     {:widget :label :schema [:qualified-keyword {:namespace :inventory.slot}]}) ; TODO one of ... == 'enum' !!

(defattribute :creature/species {:widget :label      :schema [:qualified-keyword {:namespace :species}]}) ; TODO not used ... but one of?
(defattribute :creature/level   {:widget :text-field :schema [:maybe pos-int?]}) ; pos-int-attr ? ; TODO creature lvl >0, <max-lvls (9 ?)

(defattribute :skill/start-action-sound       sound)
(defattribute :skill/action-time-modifier-key (enum :stats/cast-speed :stats/attack-speed))
(defattribute :skill/action-time              pos-attr)
(defattribute :skill/cooldown                 nat-int-attr)
(defattribute :skill/cost                     nat-int-attr)

(defattribute :world/map-size       pos-int-attr)
(defattribute :world/max-area-level pos-int-attr) ; TODO <= map-size !?
(defattribute :world/spawn-rate     pos-attr) ; TODO <1 !
