(ns cdq.entity.mana
  (:require [x.x :refer [defcomponent]]
            [cdq.entity :as entity]))

(defcomponent :entity/mana max-mana
  (entity/create-component [_]
    [max-mana max-mana]))
