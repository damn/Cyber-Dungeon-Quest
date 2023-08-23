(ns game.components.line-render
  (:require [x.x :refer [defcomponent]]
            [x.systems :refer [render]]
            [gdl.graphics.shape-drawer :as shape-drawer]))

(defcomponent :line-render {:keys [thick? end color]}
  (render [_ _ position]
    (if thick?
      (shape-drawer/with-line-width 4
        (shape-drawer/line position end color))
      (shape-drawer/line position end color))))
