(nsx game.effects.hp
  (:require [game.components.hp :refer (dead?)]
            [game.components.render :refer (hp-changed-effect)]))

; heal/mana effects
; value : [:hp [[:val :inc] 5]]
; * leech ->      [[:val :inc] x]
; * regenerate -> [[:val :inc] (percent of max)]
(effects/defeffect :hp
  {:text (fn [{:keys [value]}]
           (str value " HP"))
   :valid-params? (fn [{:keys [target]}]
                    target)
   :do! (fn [{:keys [target] :as params}]
          (let [delta (affect-val-max-stat! :hp params)]
            (hp-changed-effect target delta))
          ; TODO this can be a system somewhere which checks
          ; rule system ?
          ; not even mark destroyed ?
          (when (and (dead? @target)
                     (not (:is-player @target)))
            (swap! target assoc :destroyed? true)))})
