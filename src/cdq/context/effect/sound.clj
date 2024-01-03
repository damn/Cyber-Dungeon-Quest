(ns cdq.context.effect.sound
  (:require [gdl.context :refer [play-sound!]]
            [cdq.context.effect :as effect]))

(defmethod effect/text :effect/sound
  [_ctx _effect]
  nil)

(defmethod effect/valid-params? :effect/sound
  [_ctx _effect]
  true)

(defmethod effect/do! :effect/sound
  [ctx [_ file]]
  (play-sound! ctx file))
