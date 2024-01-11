(ns cdq.context.properties.attributes
  (:require [malli.core :as m]
            [data.val-max :refer [val-max-schema]]))

(def attributes {})

; TODO attr schema !
; * for adding entity components -> need default-value ???
; TODO all used @ :components (tx/,modifier) = add default-value
; (nested map attr needs default-value to add, but I can place this in the components right?)
; see damage
; min-max no schema?
; TODO modifer shield/damage/armor not done, validatable
; TODO one-tomany - one of spells/skills
; TODO one-tomany - one of items
; TODO body assert >+ min body size?
; TODO entity non removable! are removalbe right now!, what is optional, what depends on what?
; reaction time?
; TODO also which attribute is optional or not ? see ... default-values calculate from that?
; TODO creature lvl >0, <max-lvls (9 ?)

; TODO ___ weapon / skill / item mess ____

; TODO make misc is when no property-type matches ? :else case?

; could define attributes/property-types as data only even ... and make editable
; property-editor
; attribute-editor
; property-type-editor
; attributes.edn
; property-types.edn
; => standalone app for creating game property db & getting an editor for it for free
; big advantage: sound,animation,image, engine integration
; game property database manager
; could even be used for other stuff
; TODO namespaced attr
; one-liner, sorted alphabetically with constructor fns


(comment
 ; this is not done anymore @ tx/entity -> do it? how to?
 (def ^:private modifier-attributes (keys modifier/modifier-definitions))
 (assert (= (set (filter #(= "modifier" (namespace %)) (keys attributes)))
            (set modifier-attributes))))

(defn- defattribute [k data]
  (alter-var-root #'attributes assoc k data))

;;

(def ^:private sound        {:widget :sound      :schema :string})
(def ^:priate  image        {:widget :image      :schema :some})
(def ^:private animation    {:widget :animation  :schema :some})
(def ^:private string-attr  {:widget :text-field :schema :string})
(def ^:private boolean-attr {:widget :check-box  :schema :boolean :default-value true})
(def ^:private val-max-attr {:widget :text-field :schema (m/form val-max-schema)})

; numbers
(def ^:private nat-int-attr {:widget :text-field :schema nat-int?})
(def ^:private pos-attr     {:widget :text-field :schema pos?})
(def ^:private pos-int-attr {:widget :text-field :schema pos-int?})

(defn- enum [& items]
  {:widget :enum
   :schema (apply vector :enum items)
   :items items})

(defn- one-to-many-ids [property-type]
  {:widget :one-to-many
   :schema [:set :qualified-keyword]
   :linked-property-type property-type})

;;

(defattribute :property/image       image)
(defattribute :property/sound       sound)
(defattribute :property/pretty-name string-attr)

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

(defattribute :damage/type    (enum :physical :magic))
(defattribute :damage/min-max val-max-attr)

(defattribute :tx/damage {:widget :nested-map
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

(defattribute :maxrange {:widget :text-field}) ; TODO no schema

(defattribute :tx/target-entity {:widget :nested-map
                                 :schema [:map {:closed true}
                                          [:hit-effect [:map]]
                                          [:maxrange pos?]]
                                 :default-value {:hit-effect {}
                                                 :max-range 2.0}})

(defattribute :modifier/max-hp       {:widget :text-field :schema number?})
(defattribute :modifier/max-mana     {:widget :text-field :schema number?})
(defattribute :modifier/cast-speed   pos-attr)
(defattribute :modifier/attack-speed pos-attr)
(defattribute :modifier/shield       {:widget :text-field :schema :some})
(defattribute :modifier/armor        {:widget :text-field :schema :some})
(defattribute :modifier/damage       {:widget :text-field :schema :some})

(defn removable-attribute? [k]
  (#{"tx" "modifier" "entity"} (namespace k)))

(defn- components-attribute [component-namespace]
  (let [component-attributes (filter #(= (name component-namespace) (namespace %))
                                     (keys attributes))]
    {:widget :nested-map
     :schema (vec (concat [:map {:closed true}]
                          (for [k component-attributes]
                            [k {:optional true} (:schema (get attributes k))])))
     :components component-attributes}))

(defattribute :property/entity (components-attribute :entity))
(defattribute :skill/effect    (components-attribute :tx))
(defattribute :hit-effect      (components-attribute :tx))
(defattribute :item/modifier (components-attribute :modifier))

; TODO one of ...
(defattribute :item/slot     {:widget :label :schema [:qualified-keyword {:namespace :inventory.slot}]})

; TODO not used ... but one of?
(defattribute :creature/species {:widget :label      :schema [:qualified-keyword {:namespace :species}]})
(defattribute :creature/level   {:widget :text-field :schema [:maybe pos-int?]}) ; pos-int-attr ?

(defattribute :skill/start-action-sound       sound)
(defattribute :skill/action-time-modifier-key (enum :stats/cast-speed :stats/attack-speed))
(defattribute :skill/action-time              pos-attr)
(defattribute :skill/cooldown                 nat-int-attr)
(defattribute :skill/cost                     nat-int-attr)

(defattribute :world/map-size       pos-int-attr)
(defattribute :world/max-area-level pos-int-attr) ; TODO <= map-size !?
(defattribute :world/spawn-rate     pos-attr) ; TODO <1 !
