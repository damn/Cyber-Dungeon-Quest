(ns game.components.image
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.image :as image]
            [game.render :as render]))

(defcomponent :image image
  (render/default [_ {:keys [body]} position]
    (image/draw-rotated-centered image
                                 (if body
                                   (:rotation-angle body)
                                   0)
                                 position)))
