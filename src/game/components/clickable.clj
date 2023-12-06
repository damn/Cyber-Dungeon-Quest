(ns game.components.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.font :as font]
            [gdl.vector :as v]
            [game.entity :as entity]
            [game.player.entity :refer (player-entity)]))

(defcomponent :clickable {:keys [text]}
  (entity/render-default [_ context {[x y] :position :keys [mouseover? body]}]
    (when (and mouseover? text)
      (font/draw-text context
                      {:text text
                       :x x
                       :y (+ y (:half-height body))
                       :up? true}))))

(def ^:private click-distance-tiles 1.5)

(defmulti on-clicked (fn [stage entity] (:type (:clickable @entity))))

(defn clickable-mouseover-entity? [entity]
  (and entity
       (:clickable @entity)
       (< (v/distance (:position @player-entity)
                      (:position @entity))
          click-distance-tiles)))
