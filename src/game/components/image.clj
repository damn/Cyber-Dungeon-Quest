(ns game.components.image
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.draw :as draw]
            [game.entity :as entity]))

(defcomponent :image image
  (entity/render-default [_ drawer _ctx {:keys [position body]}]
    (draw/rotated-centered-image drawer
                                 image
                                 (if body (:rotation-angle body) 0)
                                 position)))
