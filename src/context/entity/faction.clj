(ns context.entity.faction
  (:require context.ecs
            cdq.entity))

(extend-type context.ecs.Entity
  cdq.entity/Faction
  (enemy-faction [{:keys [entity/faction]}]
    (case faction
      :faction/evil :faction/good
      :faction/good :faction/evil))

  (friendly-faction [{:keys [entity/faction]}]
    faction))
