(ns cdq.context.entity.mana
  (:require [x.x :refer [defcomponent]]
            [cdq.context.ecs :as ecs]))

(defcomponent :entity/mana max-mana
  (ecs/create [_]
    [max-mana max-mana]))
