(ns cdq.tx.stun
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [readable-number]]
            [cdq.context :refer [transact!]]
            [cdq.effect :as effect]))

(defcomponent :tx/stun duration
  (effect/text [_ _ctx]
    (str "Stuns for " (readable-number duration) " seconds"))

  (effect/valid-params? [_ {:keys [effect/source effect/target]}]
    (and target))

  (transact! [_ {:keys [effect/target]}]
    [[:tx/event target :stun duration]]))
