(ns cdq.entity.mouseover
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [with-shape-line-width draw-ellipse]]
            [cdq.api.entity :as entity]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defcomponent :entity/mouseover? {}
  _
  (entity/render-below [_
                        {:keys [entity/position entity/body entity/faction]}
                        {:keys [context/player-entity] :as ctx}]
    (with-shape-line-width ctx 3
      #(draw-ellipse ctx
                     position
                     (:half-width body)
                     (:half-height body)
                     (cond (= faction (entity/enemy-faction @player-entity))
                           enemy-color
                           (= faction (entity/friendly-faction @player-entity))
                           friendly-color
                           :else
                           neutral-color)))))
