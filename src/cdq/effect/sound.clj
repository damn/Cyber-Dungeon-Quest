(ns cdq.effect.sound
  (:require [malli.core :as m]
            [x.x :refer [defcomponent]]
            [cdq.effect :as effect]))

(def ^:private schema
  (m/schema string?))

(defcomponent :effect/sound file
  (effect/value-schema [_]
    schema)

  (effect/text [_ _ctx]
    nil)

  (effect/valid-params? [_ _ctx]
    true)

  (effect/transactions [_ _ctx]
    [[:tx/sound file]]))
