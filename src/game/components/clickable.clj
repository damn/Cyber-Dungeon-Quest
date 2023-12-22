(ns game.components.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-text]]
            [context.ecs :as entity]))

(defcomponent :entity/clickable {:keys [text]}
  (entity/render-default [_ c {[x y] :position :keys [mouseover? body]}]
    (when (and mouseover? text)
      (draw-text c
                 {:text text
                  :x x
                  :y (+ y (:half-height body))
                  :up? true}))))
