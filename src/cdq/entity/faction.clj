(ns cdq.entity.faction
  (:require [cdq.api.entity :as entity]))

(extend-type cdq.api.entity.Entity
  entity/Faction
  (enemy-faction [{:keys [entity/faction]}]
    (case faction
      :evil :good
      :good :evil))

  (friendly-faction [{:keys [entity/faction]}]
    faction))
