(ns cdq.properties
  (:require [malli.core :as m]
            [core.component :refer [defattribute]]
            [cdq.attributes :as attr]
            cdq.tx.all
            cdq.entity.all
            cdq.modifier.all))

(defattribute :property/image       attr/image)
(defattribute :property/sound       attr/sound)
(defattribute :property/pretty-name attr/string-attr)

(defattribute :property/entity (attr/components-attribute :entity))
(defattribute :skill/effect (attr/components-attribute :tx))
(defattribute :hit-effect   (attr/components-attribute :tx))
(defattribute :item/modifier (attr/components-attribute :modifier))

(defattribute :item/slot     {:widget :label :schema [:qualified-keyword {:namespace :inventory.slot}]}) ; TODO one of ... == 'enum' !!

(defattribute :creature/species {:widget :label      :schema [:qualified-keyword {:namespace :species}]}) ; TODO not used ... but one of?
(defattribute :creature/level   {:widget :text-field :schema [:maybe pos-int?]}) ; pos-int-attr ? ; TODO creature lvl >0, <max-lvls (9 ?)

(defattribute :skill/start-action-sound       attr/sound)
(defattribute :skill/action-time-modifier-key (attr/enum :stats/cast-speed :stats/attack-speed))
(defattribute :skill/action-time              attr/pos-attr)
(defattribute :skill/cooldown                 attr/nat-int-attr)
(defattribute :skill/cost                     attr/nat-int-attr)

(defattribute :world/map-size       attr/pos-int-attr)
(defattribute :world/max-area-level attr/pos-int-attr) ; TODO <= map-size !?
(defattribute :world/spawn-rate     attr/pos-attr) ; TODO <1 !

; TODO make misc is when no property-type matches ? :else case?

; TODO similar to map-attribute & components-attribute
(defn- map-attribute-schema [id-attribute attr-ks]
  (m/schema
   (vec (concat [:map {:closed true} id-attribute] ; TODO same id-attribute w. different namespaces ...
                ; creature/id ?
                ; item/id ?
                (for [k attr-ks]
                  (vector k (:schema (get core.component/attributes k))))))))

(def property-types
  {:property.type/creature {:of-type? :creature/species
                            :edn-file-sort-order 1
                            :title "Creature"
                            :overview {:title "Creatures"
                                       :columns 16
                                       :image/dimensions [65 65]
                                       :sort-by-fn #(vector (or (:creature/level %) 9)
                                                            (name (:creature/species %))
                                                            (name (:property/id %)))
                                       :extra-info-text #(str (:creature/level %)
                                                              (case (:entity/faction (:property/entity %))
                                                                :good "g"
                                                                :evil "e"))}
                            :schema (map-attribute-schema
                                     [:property/id [:qualified-keyword {:namespace :creatures}]]
                                     [:property/image
                                      :creature/species
                                      :creature/level
                                      :property/entity])}

   :property.type/skill {:of-type? :skill/effect
                         :edn-file-sort-order 0
                         :title "Skill"
                         :overview {:title "Skill"
                                    :columns 16
                                    :image/dimensions [70 70]}
                         :schema (map-attribute-schema
                                  [:property/id [:qualified-keyword {:namespace :skills}]]
                                  [:property/image
                                   :skill/action-time
                                   :skill/cooldown
                                   :skill/cost
                                   :skill/effect
                                   :skill/start-action-sound
                                   :skill/action-time-modifier-key])}

   :property.type/item {:of-type? :item/slot
                        :edn-file-sort-order 3
                        :title "Item"
                        :overview {:title "Items"
                                   :columns 17
                                   :image/dimensions [60 60]
                                   :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                          (name slot)
                                                          "")
                                                        (name (:property/id %)))}
                        :schema (map-attribute-schema
                                 [:property/id [:qualified-keyword {:namespace :items}]]
                                 [:property/pretty-name
                                  :property/image
                                  :item/slot
                                  :item/modifier])}

   ; TODO schema missing here .... world/princess key not at defattribute ... require schema ...
   :property.type/world {:of-type? :world/princess
                         :edn-file-sort-order 5
                         :title "World"
                         :overview {:title "Worlds"
                                    :columns 10
                                    :image/dimensions [96 96]}
                         #_:schema #_(map-attribute-schema
                                  [:property/id [:qualified-keyword {:namespace :worlds}]]
                                  [:world/map-size
                                   :world/max-area-level
                                   :world/princess
                                   :world/spawn-rate])}

   :property.type/misc {:of-type? (fn [{:keys [entity/hp
                                               creature/species
                                               item/slot
                                               skill/effect
                                               world/princess]}]
                                    (not (or hp species slot effect princess)))
                        :edn-file-sort-order 6
                        :title "Misc"
                        :overview {:title "Misc"
                                   :columns 10
                                   :image/dimensions [96 96]}}})
