(nsx game.components.clickable
  (:require [game.ui.mouseover-entity :refer (get-mouseover-entity)]
            [game.player.entity :refer (player-entity)]))

; TODO simpler if this would be ':mouseover-text' component simply.
(defcomponent :clickable {:keys [text]}
  (render [_ {:keys [mouseover? body]} [x y]]
    (when (and mouseover? text)
      (g/draw-text {:font media/font
                    :text text
                    :x x
                    :y (+ y (:half-height body))
                    :up? true}))))

(def ^:private click-distance-tiles 1.5)

(defmulti on-clicked (fn [entity] (:type (:clickable @entity))))

(defn check-clickable-mouseoverbody
  "Returns true if the click was processed."
  []
  (when-let [entity (get-mouseover-entity)]
    (when (and (:clickable @entity)
               (< (v/distance (:position @player-entity)
                              (:position @entity))
                  click-distance-tiles))
      (on-clicked entity)
      true)))
