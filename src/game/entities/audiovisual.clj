(ns game.entities.audiovisual
  (:require [gdl.context :refer [play-sound!]]
            [utils.core :refer [safe-get]]
            [game.context :as gm]))

(defn create! [{:keys [context/properties] :as context} position id]
  (let [{:keys [sound animation]} (safe-get properties id)]
    (play-sound! context sound)
    (gm/create-entity! context
                       {:position position
                        :animation animation
                        :z-order :effect
                        :delete-after-animation-stopped? true})))
