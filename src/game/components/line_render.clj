(ns game.components.line-render
  (:require [x.x :refer [defcomponent]]
            [gdl.protocols :refer [with-shape-line-width draw-line]]
            [game.entity :as entity]))

(defcomponent :line-render {:keys [thick? end color]}
  (entity/render-default [_ c {:keys [position]}]
    (if thick?
      (with-shape-line-width c 4
        #(draw-line c position end color))
      (draw-line c position end color))))
