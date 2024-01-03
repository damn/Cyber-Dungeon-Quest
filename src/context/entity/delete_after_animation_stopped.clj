(ns context.entity.delete-after-animation-stopped
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [context.ecs :as ecs]))

(defcomponent :entity/delete-after-animation-stopped? _
  (ecs/create! [_ e _ctx]
    (-> @e :entity/animation :looping? not assert))
  (ecs/tick! [_ e _ctx]
    (when (-> @e :entity/animation animation/stopped?)
      (swap! e assoc :entity/destroyed? true))))
