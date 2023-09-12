(ns game.effects.mana
  (:require [data.val-max :refer [affect-val-max-stat!]]
            [game.effect :as effect]
            [game.components.string-effect :refer (mana-changed-effect)]))

(effect/defeffect :mana
  {:text (fn [{:keys [value]}]
           (str value " MP"))
   :valid-params? (fn [{:keys [target]}]
                    target)
   :do! (fn [{:keys [target] :as params}]
          (let [delta (affect-val-max-stat! :mana params)]
            (mana-changed-effect target delta)))})
