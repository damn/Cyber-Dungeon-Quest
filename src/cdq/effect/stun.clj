(ns cdq.effect.stun
  (:require [malli.core :as m]
            [utils.core :refer [readable-number]]
            [cdq.effect :as effect]))

(def ^:private schema (m/schema [:and number? pos?]))

(defmethod effect/value-schema :effect/stun [_]
  schema)

(defmethod effect/text :effect/stun
  [_context [_ duration]]
  (str "Stuns for " (readable-number duration) " seconds"))

; TODO target needs to have a state component so we can send events) (actually no then just nothing happens)
(defmethod effect/valid-params? :effect/stun
  [{:keys [effect/source effect/target]} _effect]
  (and target))

(defmethod effect/transactions :effect/stun
  [{:keys [effect/target] :as context} [_ duration]]
  [:tx/event target :stun duration])
