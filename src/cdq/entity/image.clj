(ns cdq.entity.image
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-rotated-centered-image]]
            [cdq.entity :as entity]))

(defcomponent :entity/image image
  (entity/render-default [_ {:keys [entity/position entity/body]} c]
    (draw-rotated-centered-image c
                                 image
                                 (if body (:rotation-angle body) 0)
                                 position)))
