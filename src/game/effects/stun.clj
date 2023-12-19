(ns game.effects.stun
  (:require [utils.core :refer [readable-number]]
            [data.counter :as counter]
            [game.effect :as effect]
            [game.components.state :as state]))

(effect/defeffect :stun
  {:text (fn [duration _params _context]
           (str "Stuns for " (readable-number (/ duration 1000)) " seconds"))
   :valid-params? (fn [_effect-val {:keys [source target]} _context]
                    (and target)) ; TODO target needs to have a state component so we can send events
   :do! (fn [duration {:keys [target]} context]
          (state/send-event! context target :stun duration))})
