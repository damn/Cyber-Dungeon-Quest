(ns cdq.entity.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics :as g]
            [cdq.api.entity :as entity]))

(defcomponent :entity/clickable {}
  {:keys [text]}
  (entity/render-default [_ {[x y] :entity/position :keys [entity/mouseover? entity/body]} g _ctx]
    (when (and mouseover? text)
      (g/draw-text g
                   {:text text
                    :x x
                    :y (+ y (:half-height body))
                    :up? true}))))
