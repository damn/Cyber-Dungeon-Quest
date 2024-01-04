(ns cdq.context.effect.sound
  (:require [malli.core :as m]
            [gdl.context :refer [play-sound!]]
            [cdq.context.effect :as effect]))

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

(defmethod effect/do! :effect/sound
  [ctx [_ file]]
  (play-sound! ctx file))
