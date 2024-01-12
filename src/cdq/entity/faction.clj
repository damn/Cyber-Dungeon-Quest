(ns cdq.entity.faction
  (:require [cdq.entity :as entity]))

(extend-type cdq.entity.Entity
  entity/Faction
  (enemy-faction [{:keys [entity/faction]}]
    (case faction
      :evil :good
      :good :evil))

  (friendly-faction [{:keys [entity/faction]}]
    faction))
