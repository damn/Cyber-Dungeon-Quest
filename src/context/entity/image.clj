(ns context.entity.image
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-rotated-centered-image]]
            [context.ecs :as ecs]))

(defcomponent :entity/image image
  (ecs/render-default [_ {:keys [entity/position entity/body]} c]
    (draw-rotated-centered-image c
                                 image
                                 (if body (:rotation-angle body) 0)
                                 position)))
