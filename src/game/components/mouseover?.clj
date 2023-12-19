(ns game.components.mouseover?
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.color :as color]
            [gdl.context :refer [with-shape-line-width draw-ellipse]]
            [context.ecs :as entity]))

(def ^:private outline-alpha 0.4)
(color/defrgb ^:private enemy-color    1 0 0 outline-alpha)
(color/defrgb ^:private friendly-color 0 1 0 outline-alpha)
(color/defrgb ^:private neutral-color  1 1 1 outline-alpha)

(defcomponent :mouseover? _
  (entity/render-below [_ c {:keys [position body faction]}]
    (with-shape-line-width c 3
      #(draw-ellipse c position
                     (:half-width body)
                     (:half-height body)
                     (case faction ; TODO enemy faction of player
                       :evil friendly-color
                       :good enemy-color
                       neutral-color)))))
