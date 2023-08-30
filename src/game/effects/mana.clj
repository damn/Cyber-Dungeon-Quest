(ns game.effects.mana
  (:require [utils.core :refer [affect-val-max-stat!]]
            [game.effects.core :as effects]
            [game.components.string-effect :refer (mana-changed-effect)]))

(effects/defeffect :mana
  {:text (fn [{:keys [value]}]
           (str value " MP"))
   :valid-params? (fn [{:keys [target]}]
                    target)
   :do! (fn [{:keys [target] :as params}]
          (let [delta (affect-val-max-stat! :mana params)]
            (mana-changed-effect target delta)))})
