(ns context.entity.faction
  (:require context.entity
            game.entity))

(extend-type context.entity.Entity
  game.entity/Faction
  (enemy-faction [{:keys [faction]}]
    (case faction
      :evil :good
      :good :evil)))
