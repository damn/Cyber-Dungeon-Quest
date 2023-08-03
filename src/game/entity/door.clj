(nsx game.entity.door
  (:require [game.components.clickable :as clickable])
  (:use
    (game.components position body render)
    (game.maps cell-grid)))

; TODO render on minimap

; another way of handling the open door was to change the image&mouseover-outline to false&remove component :pressable
; but it had still a :body component which blocks mouseoverentities that lie below it because there is always only 1 mouseoverbody at each position
; ... or remove the :body component too?
#_(defn ^:private make-open-door [p image]
  (create-entity!
   {:position p
    :z-order :ground
    :image image}))

#_(defmethod clickable/on-clicked :door [entity]
  (when-not (:is-clicked @entity)
    (swap! entity assoc :is-clicked true)
    (audio/play "ReversyH-Nick_Ros-105.wav")
    (swap! entity assoc :destroyed? true)
    (make-open-door p open-image)
    (change-cell-blocks (get-cell p) #{})
    (cell-blocks-changed-update-listeners)))

#_(defn make-door [p closed-image open-image] ; make-closed-door ?
  (create-entity!
   {:position p
    :body {:width 1
           :height 1
           :is-solid false}
    :z-order :ground
    :image closed-image
    :is-clicked false
    :clickable {:type :door}}))
