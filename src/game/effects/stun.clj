(ns game.effects.stun
  (:require [utils.core :refer [readable-number]]
            [game.context :refer [send-event!]]
            [game.effect :as effect]))

(defmethod effect/text :stun
  [_context [_ duration]]
  (str "Stuns for " (readable-number (/ duration 1000)) " seconds"))

(defmethod effect/valid-params?
  [{:keys [source target]} _this]
  (and target)); TODO target needs to have a state component so we can send events)

(defmethod effect/do! :stun
  [{:keys [target]} [_ duration]]
  (send-event! context target :stun duration))
