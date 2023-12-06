(ns game.components.image
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.image :as image]
            [game.entity :as entity]))

(defcomponent :image image
  (entity/render-default [_ context {:keys [position body]}]
    (image/draw-rotated-centered context
                                 image
                                 (if body (:rotation-angle body) 0)
                                 position)))
