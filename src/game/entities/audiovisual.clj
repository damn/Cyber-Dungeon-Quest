(ns game.entities.audiovisual
  (:require [gdl.context :refer [play-sound! get-property]]
            [game.context :as gm]))

(defn create! [context position id]
  (let [{:keys [sound animation]} (get-property context id)]
    (play-sound! context sound)
    (gm/create-entity! context
                       {:position position
                        :animation animation
                        :z-order :effect
                        :delete-after-animation-stopped? true})))
