(nsx game.entities.chest
  (:require [game.components.clickable :as clickable]
            [game.maps.data :refer (get-current-map-data)]))

#_(defmethod clickable/on-clicked :chest [entity]
  (audio/play "bfxr_chestopen.wav")
  (swap! entity assoc :destroyed? true)
  (if-let [item-name (:item-name @entity)]
    ; TODO pass item instance
    (item-entity/create! (:position @entity) item-name)
    (item-entity/create! (:position @entity) :max-lvl (:rand-item-max-lvl (get-current-map-data)))))

#_(defn create-chest [position & {item-name :item-name}]
  (db/create-entity!
   {:position position
    :body {:width 1
           :height 1
           :is-solid true}
    :image (media/get-itemsprite [7 1])
    :item-name item-name
    :clickable {:type :chest}}))
