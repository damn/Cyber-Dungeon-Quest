(ns game.entities.audiovisual
  (:require [utils.core :refer [safe-get]]
            [game.context :as gm])
  (:import com.badlogic.gdx.audio.Sound))

(defn create! [{:keys [assets context/properties] :as context} position id]
  (let [{:keys [sound animation]} (safe-get properties id)]
    (.play ^Sound (get assets sound))
    (gm/create-entity! context
                       {:position position
                        :animation animation
                        :z-order :effect
                        :delete-after-animation-stopped? true})))
