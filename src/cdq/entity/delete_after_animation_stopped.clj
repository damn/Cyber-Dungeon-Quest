(ns cdq.entity.delete-after-animation-stopped
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.entity :as entity]))

(defcomponent :entity/delete-after-animation-stopped? _
  (entity/create! [_ e _ctx]
    (-> @e :entity/animation :looping? not assert))
  (entity/tick [_ entity* _ctx]
    (when (-> entity* :entity/animation animation/stopped?)
      [(assoc entity* :entity/destroyed? true)])))
