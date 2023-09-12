(ns game.effects.mana
  (:require [data.val-max :refer [affect-val-max-stat!]]
            [game.effect :as effect]
            [game.components.string-effect :refer (mana-changed-effect)]))

(effect/defeffect :mana
  {:text (fn [{:keys [value]}]
           (str value " MP"))
   :valid-params? (fn [{:keys [target]}]
                    target)
   :do! (fn [{:keys [target value]}]
          (let [modifier value
                delta (affect-val-max-stat! :at-key :mana
                                            :entity target
                                            :modifier modifier)]
            (mana-changed-effect target delta)))})
