(ns game.components.image
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.image :as image]
            [game.render :as render]))

(defcomponent :image image
  (render/default [_c {:keys [body]} position]
    (image/draw-rotated-centered image
                                 (or (:rotation-angle body) 0)
                                 position)))

(comment
 (defcomponent :image v
   (render [_ {:keys [rotation-angle position]}]
     (image/draw-rotated-centered v (or rotation-angle 0) position))))

; first arg [k v] = c = component = this
