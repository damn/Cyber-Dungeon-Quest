(ns context.entity.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-text]]
            [context.entity :as entity]))

(defcomponent :entity/clickable {:keys [text]}
  (entity/render-default [_ {[x y] :entity/position :keys [entity/mouseover? entity/body]} c]
    (when (and mouseover? text)
      (draw-text c
                 {:text text
                  :x x
                  :y (+ y (:half-height body))
                  :up? true}))))
