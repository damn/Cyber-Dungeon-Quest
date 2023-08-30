(ns game.components.mana
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [val-max apply-max]]
            [game.db :as db]
            [game.components.modifiers :as modifiers]))

(defcomponent :mana max-mana
  (db/create [_]
    (val-max max-mana)))

(modifiers/defmodifier :mana
  {:values  [[15 25] [35 45] [55 65]] ; TODO values ?
   :text    #(str "+" % " Mana")
   :keys    [:mana]
   :apply   (partial apply-max +)
   :reverse (partial apply-max -)})
