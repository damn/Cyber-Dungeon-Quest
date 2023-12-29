(ns context.effect.stun
  (:require [utils.core :refer [readable-number]]
            [cdq.context :refer [send-event!]]
            [context.effect :as effect]))

(defmethod effect/text :effect/stun
  [_context [_ duration]]
  (str "Stuns for " (readable-number duration) " seconds"))

; TODO target needs to have a state component so we can send events) (actually no then just nothing happens)
(defmethod effect/valid-params? :effect/stun
  [{:keys [effect/source effect/target]} _effect]
  (and target))

(defmethod effect/do! :effect/stun
  [{:keys [effect/target] :as context} [_ duration]]
  (send-event! context target :stun duration))
