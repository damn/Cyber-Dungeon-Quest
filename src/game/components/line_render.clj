(ns game.components.line-render
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [game.systems :refer [render]]))

(defcomponent :line-render {:keys [thick? end color]}
  (render [_c _ position] ; two times underscore will shadow the destructuring! TODO FIXME
    (if thick?
      (shape-drawer/with-line-width 4
        (shape-drawer/line position end color))
      (shape-drawer/line position end color))))