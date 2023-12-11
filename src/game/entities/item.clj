(ns game.entities.item
  (:require [game.db :as db]
            [game.components.clickable :as clickable]
            [game.components.inventory :as inventory]
            [game.utils.msg-to-player :refer [show-msg-to-player]]
            [game.player.entity :refer [player-entity]])
  (:import com.badlogic.gdx.audio.Sound
           com.badlogic.gdx.scenes.scene2d.Actor))

(defmethod clickable/on-clicked :item [{:keys [stage assets]} entity]
  (let [item (:item @entity)]
    (cond
     (.isVisible ^Actor (:inventory-window stage))
     (do
      (.play ^Sound (get assets "sounds/bfxr_takeit.wav"))
      (swap! entity assoc :destroyed? true)
      (swap! player-entity assoc :item-on-cursor item))

     (inventory/try-pickup-item! player-entity item)
     (do
      (.play ^Sound (get assets "sounds/bfxr_pickup.wav"))
      (swap! entity assoc :destroyed? true))

     :else
     (do
      (.play ^Sound (get assets "sounds/bfxr_denied.wav"))
      (show-msg-to-player "Your Inventory is full")))))

; TODO use image w. shadows spritesheet
(defn create! [position item]
  (db/create-entity!
   {:position position
    :body {:width 0.5 ; TODO use item-body-dimensions
           :height 0.5
           :is-solid false} ; solid? collides?
    :z-order :on-ground
    :image (:image item)
    :item item
    ; :mouseover-text (:pretty-name item)
    ; :clickable :item
    :clickable {:type :item
                :text (:pretty-name item)}})) ; TODO item-color also from text...? uniques/magic/...
