(ns cdq.entity.mana
  (:require [x.x :refer [defcomponent]]
            [cdq.entity :as entity]))

(defcomponent :entity/mana max-mana
  (entity/create [_]
    [max-mana max-mana]))
