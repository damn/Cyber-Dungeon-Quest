(ns cdq.effect.sound
  (:require [malli.core :as m]
            [cdq.effect :as effect]))

(def ^:private schema
  (m/schema string?))

(defmethod effect/value-schema :effect/sound [_]
  schema)

(defmethod effect/text :effect/sound
  [_ctx _effect]
  nil)

(defmethod effect/valid-params? :effect/sound
  [_ctx _effect]
  true)

(defmethod effect/transactions :effect/sound
  [ctx [_ file]]
  [:tx/sound file])
