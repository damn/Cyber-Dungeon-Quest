(ns context.entity.mana
  (:require [x.x :refer [defcomponent]]
            [context.ecs :as entity]))

(defcomponent :mana max-mana
  (entity/create [_]
    [max-mana max-mana]))
