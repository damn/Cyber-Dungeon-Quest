(nsx game.components.mana)

(defcomponent :mana max-mana
  (create [_]
    (val-max max-mana)))

(modifiers/defmodifier :mana
  {:values  [[15 25] [35 45] [55 65]] ; TODO values ?
   :text    #(str "+" % " Mana")
   :keys    [:mana]
   :apply   (partial apply-max +)
   :reverse (partial apply-max -)})
