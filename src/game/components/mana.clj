(ns game.components.mana
  (:require [x.x :refer [defcomponent]]
            [game.entity :as entity]))

(defcomponent :mana max-mana
  (entity/create [_]
    [max-mana max-mana]))
