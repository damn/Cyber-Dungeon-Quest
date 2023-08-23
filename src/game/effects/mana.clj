(nsx game.effects.mana
  (:require [game.components.render :refer (mana-changed-effect)]))

(println " REQUIRE GAME EFFECTS MANA ")

(effects/defeffect :mana
  {:text (fn [{:keys [value]}]
           (str value " MP"))
   :valid-params? (fn [{:keys [target]}]
                    target)
   :do! (fn [{:keys [target] :as params}]
          (let [delta (affect-val-max-stat! :mana params)]
            (mana-changed-effect target delta)))})
