(ns game.entities.item
  (:require [gdl.audio :as audio]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui :as ui]
            [game.db :as db]
            [game.components.clickable :as clickable]
            [game.components.inventory :as inventory]
            [game.utils.msg-to-player :refer [show-msg-to-player]]
            [game.player.entity :refer [player-entity]]))

(defmethod clickable/on-clicked :item [stage entity]
  (let [item (:item @entity)]
    (when-not @inventory/item-in-hand
      (cond
       (actor/visible? (:inventory-window stage))
       (do
        (audio/play "bfxr_takeit.wav")
        (swap! entity assoc :destroyed? true)
        (inventory/set-item-in-hand item))

       (inventory/try-pickup-item player-entity item)
       (do
        (audio/play "bfxr_pickup.wav")
        (swap! entity assoc :destroyed? true))

       :else
       (do
        (audio/play "bfxr_denied.wav")
        ; (msg-to-player/show "Your inventory is full")
        (show-msg-to-player "Your Inventory is full"))))))

; TODO use image w. shadows spritesheet
(defn create! [position item]
  (db/create-entity!
   {:position position
    :body {:width 0.5 ; TODO use item-body-dimensions
           :height 0.5
           :is-solid false} ; solid? collides?
    :z-order :on-ground
    :image (:image item)
    ;:glittering true ; no animation (deleted it) TODO ?? (didnt work with pausable)
    :item item
    ; :mouseover-text (:pretty-name item)
    ; :clickable :item
    :clickable {:type :item
                :text (:pretty-name item)}})) ; TODO item-color also from text...? uniques/magic/...