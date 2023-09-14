(ns game.effects.mana
  (:require [data.val-max :refer [affect-val-max-stat!]]
            [game.effect :as effect]
            [game.components.string-effect :refer (mana-changed-effect)]))

(effect/defeffect :mana
  {:text (fn [modifier _]
           (str modifier " MP"))
   :valid-params? (fn [_ {:keys [target]}]
                    target)
   :do! (fn [modifier {:keys [target]}]
          (let [delta (affect-val-max-stat! :at-key :mana
                                            :entity target
                                            :modifier modifier)]
            (mana-changed-effect target delta)))})
