(ns cdq.context.entity.line-render
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [with-shape-line-width draw-line]]
            [cdq.context.ecs :as ecs]))

(defcomponent :entity/line-render {:keys [thick? end color]}
  (ecs/render-default [_ {:keys [entity/position]} c]
    (if thick?
      (with-shape-line-width c 4
        #(draw-line c position end color))
      (draw-line c position end color))))