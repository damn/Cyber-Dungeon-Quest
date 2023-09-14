(ns game.effects.hp
  (:require [data.val-max :refer [affect-val-max-stat!]]
            [game.effect :as effect]
            [game.components.hp :refer (dead?)]
            [game.components.string-effect :refer (hp-changed-effect)]))

; heal/mana effects
; value : [:hp [[:val :inc] 5]]
; * leech ->      [[:val :inc] x]
; * regenerate -> [[:val :inc] (percent of max)]
(effect/defeffect :hp
  {:text (fn [modifier _]
           (str modifier " HP"))
   :valid-params? (fn [_ {:keys [target]}]
                    target)
   :do! (fn [modifier {:keys [target]}]
          (let [delta (affect-val-max-stat! :at-key :hp
                                            :entity target
                                            :modifier modifier)]
            (hp-changed-effect target delta))
          (when (and (dead? @target)
                     (not (:is-player @target)))
            (swap! target assoc :destroyed? true)))})
