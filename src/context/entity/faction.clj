(ns context.entity.faction
  (:require context.entity
            cdq.entity))

(extend-type context.entity.Entity
  cdq.entity/Faction
  (enemy-faction [{:keys [entity/faction]}]
    (case faction
      :evil :good
      :good :evil)))
