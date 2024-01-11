(ns cdq.context.properties.attributes
  (:require [malli.core :as m]
            [data.val-max :refer [val-max-schema]]))

; TODO attributes themself schema & namespaced keys & data-based attribute editor?
; TODO entity components, what subset is allowed? which components depend on which? which can not be removed?
; TODO namespaced attr all sub-attrs.
; TODO one-liner, use constructor for type, sorted alphabetically with constructor fns

(comment
 ; this is not done anymore @ tx/entity -> do it? how to?
 (def ^:private modifier-attributes (keys modifier/modifier-definitions))
 (assert (= (set (filter #(= "modifier" (namespace %)) (keys attributes)))
            (set modifier-attributes))))

;;

(def attributes {})

(defn- defattribute [k data]
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
(defn- one-to-many-ids [property-type]
  {:widget :one-to-many
   :schema [:set :qualified-keyword]
   :linked-property-type property-type})

(defn- components-attribute [component-namespace]
  (let [component-attributes (filter #(= (name component-namespace) (namespace %))
                                     (keys attributes))]
    {:widget :nested-map
     :schema (vec (concat [:map {:closed true}]
                          (for [k component-attributes]
                            [k {:optional true} (:schema (get attributes k))])))
     :components component-attributes}))

(defn removable-component? [k]
  (#{"tx" "modifier" "entity"} (namespace k)))

;;

(defattribute :property/image       image)
(defattribute :property/sound       sound)
(defattribute :property/pretty-name string-attr)

; TODO body assert >+ min body size?
(defattribute :entity/body {:widget :nested-map ; -> reuse map-attribute-schema, just list nested attributes
                            :schema [:map {:closed true}
                                     [:width pos?]
                                     [:height pos?]
                                     [:solid? :boolean]]
                            :default-value {:width 0.5
                                            :height 0.5
                                            :solid? true}})
(defattribute :entity/skills        (one-to-many-ids :property.type/spell))
(defattribute :entity/inventory     (one-to-many-ids :property.type/item))
(defattribute :entity/animation     animation)
(defattribute :entity/mana          nat-int-attr)
(defattribute :entity/flying?       boolean-attr)
(defattribute :entity/hp            pos-int-attr)
(defattribute :entity/movement      pos-attr)
(defattribute :entity/reaction-time pos-attr)
(defattribute :entity/faction       (enum :good :evil))

(defattribute :property/entity (components-attribute :entity))

; TODO not used @ tx/damage map attr
(defattribute :damage/type    (enum :physical :magic))
(defattribute :damage/min-max val-max-attr)

(defattribute :tx/damage          {:widget :nested-map
                                   :schema [:map {:closed true}
                                            [:damage/type [:enum :physical :magic]]
                                            [:damage/min-max (m/form val-max-schema)]]
                                   :default-value {:damage/type :physical
                                                   :damage/min-max [1 10]}})
(defattribute :tx/sound           sound)
(defattribute :tx/spawn           {:widget :text-field :schema [:qualified-keyword {:namespace :creatures}]}) ; => one to one attr!
(defattribute :tx/stun            pos-attr)
(defattribute :tx/restore-hp-mana {:widget :text-field :schema [:= true]}) ; TODO no schema
(defattribute :tx/projectile      {:widget :text-field :schema [:= true]})
(defattribute :maxrange           {:widget :text-field}) ; TODO no schema
(defattribute :tx/target-entity   {:widget :nested-map
                                   :schema [:map {:closed true}
                                            [:hit-effect [:map]] ; TODO circular depdenency components-attribute
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
(defattribute :item/slot     {:widget :label :schema [:qualified-keyword {:namespace :inventory.slot}]}) ; TODO one of ...

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
