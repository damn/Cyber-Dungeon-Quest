(ns game.entities.audiovisual
  (:require [game.db :as db]
            [game.properties :as properties])
  (:import com.badlogic.gdx.audio.Sound))

(defn create! [{:keys [assets] :as context} position id]
  (let [{:keys [sound animation]} (properties/get id)]
    (.play ^Sound (get assets sound))
    (db/create-entity! {:position position
                        :animation animation
                        :z-order :effect
                        :delete-after-animation-stopped? true}
                       context)))
