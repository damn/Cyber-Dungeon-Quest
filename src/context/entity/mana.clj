(ns context.entity.mana
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as ecs]))

(defcomponent :entity/mana max-mana
  (ecs/create [_]
    [max-mana max-mana]))
