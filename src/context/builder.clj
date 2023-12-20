(ns context.builder
  (:require [gdl.context :refer [play-sound! get-property]]
            [game.context :refer [create-entity!]])
  )

(extend-type gdl.context.Context
  game.context/Builder
  (audiovisual [context position property-id]
    (let [{:keys [sound animation]} (get-property context property-id)]
      (play-sound! context sound)
      (create-entity! context
                      {:position position
                       :animation animation
                       :z-order :effect
                       :delete-after-animation-stopped? true}))))
