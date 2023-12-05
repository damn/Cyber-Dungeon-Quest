(ns game.components.image
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.image :as image]
            [game.entity :as entity]))

(defcomponent :image image
  (entity/render-default [_ {:keys [body]} position]
    (image/draw-rotated-centered image
                                 (if body
                                   (:rotation-angle body)
                                   0)
                                 position)))
