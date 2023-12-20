(ns game.effects.stun
  (:require [utils.core :refer [readable-number]]
            [data.counter :as counter]
            [game.context :refer [send-event!]]
            [game.effect :as effect]))

(effect/component :stun
  {:text (fn [_context duration _params]
           (str "Stuns for " (readable-number (/ duration 1000)) " seconds"))
   :valid-params? (fn [_context _effect-val {:keys [source target]}]
                    (and target)) ; TODO target needs to have a state component so we can send events
   :do! (fn [context duration {:keys [target]}]
          (send-event! context target :stun duration))})
