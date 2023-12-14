(ns game.entities.audiovisual
  (:require [utils.core :refer [safe-get]]
            [game.db :as db])
  (:import com.badlogic.gdx.audio.Sound))

(defn create! [{:keys [assets context/properties] :as context} position id]
  (let [{:keys [sound animation]} (safe-get properties id)]
    (.play ^Sound (get assets sound))
    (db/create-entity! {:position position
                        :animation animation
                        :z-order :effect
                        :delete-after-animation-stopped? true}
                       context)))
