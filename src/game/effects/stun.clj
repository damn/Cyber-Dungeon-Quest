(ns game.effects.stun
  (:require [utils.core :refer [readable-number]]
            [data.counter :as counter]
            [game.context :refer [send-event!]]
            [game.effect :as effect]))

(effect/defeffect :stun
  {:text (fn [duration _params _context]
           (str "Stuns for " (readable-number (/ duration 1000)) " seconds"))
   :valid-params? (fn [_effect-val {:keys [source target]} _context]
                    (and target)) ; TODO target needs to have a state component so we can send events
   :do! (fn [duration {:keys [target]} context]
          (send-event! context target :stun duration))})
