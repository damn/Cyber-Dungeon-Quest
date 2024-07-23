(ns cdq.tx.stun
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [readable-number]]
            [cdq.api.context :refer [transact!]]
            [cdq.api.effect :as effect]
            [cdq.attributes :as attr]))

(defcomponent :tx/stun attr/pos-attr
  duration
  (effect/text [_ _ctx]
    (str "Stuns for " (readable-number duration) " seconds"))

  (effect/valid-params? [_ {:keys [effect/source effect/target]}]
    (and target))

  (transact! [_ {:keys [effect/target]}]
    [[:tx/event target :stun duration]]))
