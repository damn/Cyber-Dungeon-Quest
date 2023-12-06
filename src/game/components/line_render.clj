(ns game.components.line-render
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [game.entity :as entity]))

(defcomponent :line-render {:keys [thick? end color]}
  (entity/render-default [_ context {:keys [position]}]
    (if thick?
      (shape-drawer/with-line-width 4
        (shape-drawer/line position end color))
      (shape-drawer/line position end color))))
