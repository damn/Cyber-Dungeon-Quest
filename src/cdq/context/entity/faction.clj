(ns cdq.context.entity.faction
  (:require cdq.entity))

(extend-type cdq.entity.Entity
  cdq.entity/Faction
  (enemy-faction [{:keys [entity/faction]}]
    (case faction
      :faction/evil :faction/good
      :faction/good :faction/evil))

  (friendly-faction [{:keys [entity/faction]}]
    faction))
