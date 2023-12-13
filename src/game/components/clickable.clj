(ns game.components.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.draw :as draw]
            [gdl.vector :as v]
            [game.entity :as entity]
            [game.player.entity :refer (player-entity)]))

(defcomponent :clickable {:keys [text]}
  (entity/render-default [_ drawer _ctx {[x y] :position :keys [mouseover? body]}]
    (when (and mouseover? text)
      (draw/text drawer
                 {:text text
                  :x x
                  :y (+ y (:half-height body))
                  :up? true}))))

(def ^:private click-distance-tiles 1.5)

(defmulti on-clicked (fn [_context entity] (:type (:clickable @entity))))

(defn clickable-mouseover-entity? [entity]
  (and entity
       (:clickable @entity)
       (< (v/distance (:position @player-entity)
                      (:position @entity))
          click-distance-tiles)))
