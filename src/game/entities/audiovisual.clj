(ns game.entities.audiovisual
  (:require [gdl.assets :as assets]
            [game.db :as db]
            [game.properties :as properties]))

(defn create! [position id]
  (let [{:keys [sound animation]} (properties/get id)]
    (.play (assets/get-sound sound))
    (db/create-entity!
     {:position position
      :animation animation
      :z-order :effect
      :delete-after-animation-stopped? true})))
