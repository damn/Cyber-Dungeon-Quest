(ns game.entities.item
  (:require [game.context :as gm]))

; TODO use image w. shadows spritesheet
(defn create! [position item context]
  (gm/create-entity! context
                     {:position position
                      :body {:width 0.5 ; TODO use item-body-dimensions
                             :height 0.5
                             :is-solid false} ; solid? collides?
                      :z-order :on-ground
                      :image (:image item)
                      :item item
                      ; :mouseover-text (:pretty-name item)
                      ; :clickable :item
                      :clickable {:type :item; TODO item-color also from text...? uniques/magic/... TODO namespaced keyword.
                                  :text (:pretty-name item)}}))
