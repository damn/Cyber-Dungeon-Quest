(ns game.effects.stun
  (:require [utils.core :refer [readable-number]]
            [game.context :refer [send-event!]]
            [context.effect-interpreter :as effect]))

(defmethod effect/text :stun
  [_context [_ duration]]
  (str "Stuns for " (readable-number (/ duration 1000)) " seconds"))

; TODO target needs to have a state component so we can send events) (actually no then just nothing happens)
(defmethod effect/valid-params?
  [{:keys [effect/source
           effect/target]}
   _this]
  (and target))

(defmethod effect/do! :stun
  [{:keys [effect/target]}
   [_ duration]]
  (send-event! context target :stun duration))
