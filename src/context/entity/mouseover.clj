(ns context.entity.mouseover
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [with-shape-line-width draw-ellipse]]
            [context.entity :as entity]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defcomponent :mouseover? _
  (entity/render-below [_ {:keys [position body entity/faction]} c]
    (with-shape-line-width c 3
      #(draw-ellipse c position
                     (:half-width body)
                     (:half-height body)
                     (case faction ; TODO enemy faction of player
                       :evil friendly-color
                       :good enemy-color
                       neutral-color)))))
