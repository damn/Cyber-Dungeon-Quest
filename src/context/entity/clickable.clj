(ns context.entity.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-text]]
            [context.ecs :as ecs]))

(defcomponent :entity/clickable {:keys [text]}
  (ecs/render-default [_ {[x y] :entity/position :keys [entity/mouseover? entity/body]} c]
    (when (and mouseover? text)
      (draw-text c
                 {:text text
                  :x x
                  :y (+ y (:half-height body))
                  :up? true}))))
