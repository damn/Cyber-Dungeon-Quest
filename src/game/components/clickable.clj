(ns game.components.clickable
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.font :as font]
            [gdl.vector :as v]
            [game.render :as render]
            [game.media :as media]
            [game.player.entity :refer (player-entity)]))

; TODO simpler if this would be ':mouseover-text' component simply.
; TODO similar to show-string-effect
(defcomponent :clickable {:keys [text]}
  (render/default [_ {:keys [mouseover? body]} [x y]]
    (when (and mouseover? text)
      (font/draw-text {:font media/font
                       :text text
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
