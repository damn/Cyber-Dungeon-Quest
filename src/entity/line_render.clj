(ns entity.line-render
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [with-shape-line-width draw-line]]
            [context.ecs :as entity]))

(defcomponent :line-render {:keys [thick? end color]}
  (entity/render-default [_ {:keys [position]} c]
    (if thick?
      (with-shape-line-width c 4
        #(draw-line c position end color))
      (draw-line c position end color))))
