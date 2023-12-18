(ns game.components.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.protocols :refer [draw-text]]
            [gdl.math.vector :as v]
            [game.entity :as entity]))

(defcomponent :clickable {:keys [text]}
  (entity/render-default [_ c {[x y] :position :keys [mouseover? body]}]
    (when (and mouseover? text)
      (draw-text c
                 {:text text
                  :x x
                  :y (+ y (:half-height body))
                  :up? true}))))

(def ^:private click-distance-tiles 1.5)

(defmulti on-clicked (fn [_context _stage entity]
                       (:type (:clickable @entity))))

(defn clickable-mouseover-entity? [player-entity* mouseover-entity]
  (and mouseover-entity
       (:clickable @mouseover-entity)
       (< (v/distance (:position player-entity*)
                      (:position @mouseover-entity))
          click-distance-tiles)))
