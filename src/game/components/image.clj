(ns game.components.image
  (:require [x.x :refer [defcomponent]]
            [gdl.protocols :refer [draw-rotated-centered-image]]
            [game.entity :as entity]))

(defcomponent :image image
  (entity/render-default [_ c {:keys [position body]}]
    (draw-rotated-centered-image c
                                 image
                                 (if body (:rotation-angle body) 0)
                                 position)))
