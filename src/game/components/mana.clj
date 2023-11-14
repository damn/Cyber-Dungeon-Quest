(ns game.components.mana
  (:require [x.x :refer [defcomponent]]
            [game.db :as db]))

(defcomponent :mana max-mana
  (db/create [_]
    [max-mana max-mana]))
