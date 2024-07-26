(ns cdq.entity.faction
  (:require [core.component :refer [defattribute]]
            [cdq.api.entity :as entity]
            [cdq.attributes :as attr]))

(defattribute :entity/faction (attr/enum :good :evil))

(extend-type cdq.api.entity.Entity
  entity/Faction
  (enemy-faction [{:keys [entity/faction]}]
    (case faction
      :evil :good
      :good :evil))

  (friendly-faction [{:keys [entity/faction]}]
    faction))
