(ns cdq.entity.mana
  (:require [x.x :refer [defcomponent]]
            [cdq.api.entity :as entity]
            [cdq.attributes :as attr]))

; required @ npc state, for cost, check if nil
(defcomponent :entity/mana attr/nat-int-attr
  max-mana
  (entity/create-component [_ _components _ctx]
    [max-mana max-mana]))
