(ns cdq.context.entity.delete-after-animation-stopped
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.context.ecs :as ecs]))

(defcomponent :entity/delete-after-animation-stopped? _
  (ecs/create! [_ e _ctx]
    (-> @e :entity/animation :looping? not assert))
  (ecs/tick [_ entity* _ctx]
    (when (-> entity* :entity/animation animation/stopped?)
      (assoc entity* :entity/destroyed? true))))
