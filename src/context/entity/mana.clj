(ns context.entity.mana
  (:require [x.x :refer [defcomponent]]
            [context.entity :as entity]))

(defcomponent :entity/mana max-mana
  (entity/create [_]
    [max-mana max-mana]))
