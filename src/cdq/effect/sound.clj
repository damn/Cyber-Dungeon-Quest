(ns cdq.effect.sound
  (:require [x.x :refer [defcomponent]]
            [cdq.effect :as effect]))

(defcomponent :effect/sound file
  (effect/text [_ _ctx]
    nil)

  (effect/valid-params? [_ _ctx]
    true)

  (effect/transactions [_ _ctx]
    [[:tx/sound file]]))
