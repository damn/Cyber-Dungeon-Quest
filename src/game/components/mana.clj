(ns game.components.mana
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [val-max]]
            [game.db :as db]))

(defcomponent :mana max-mana
  (db/create [_]
    (val-max max-mana)))
