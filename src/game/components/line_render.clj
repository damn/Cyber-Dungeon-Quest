(ns game.components.line-render
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.draw :as draw]
            [game.entity :as entity]))

(defcomponent :line-render {:keys [thick? end color]}
  (entity/render-default [_ drawer _ctx {:keys [position]}]
    (if thick?
      (draw/with-line-width drawer 4
        #(draw/line drawer position end color))
      (draw/line drawer position end color))))
