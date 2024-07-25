(ns cdq.entity.image
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics :as g]
            [cdq.api.entity :as entity]))


; documentation for attributes => generate from code and just make .html or .md file
; also document each system !!! and tell each component what it does @ each system ....

(defcomponent :entity/image {}
  image
  (entity/render-default
    ; image is rendered at entity/position centered with [:rotation-angle :entity/body]  (if available)
    [_ {:keys [entity/position entity/body]} g _ctx]
    (g/draw-rotated-centered-image g
                                   image
                                   (if body (:rotation-angle body) 0)
                                   position)))
