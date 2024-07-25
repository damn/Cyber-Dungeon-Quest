(ns cdq.entity.image
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics :as g]
            [cdq.api.entity :as entity]))

(defcomponent :entity/image {}
  image
  (entity/render-default
    [_ {:keys [entity/position entity/body]} g _ctx]
    (g/draw-rotated-centered-image g
                                   image
                                   (if body (:rotation-angle body) 0)
                                   position)))
