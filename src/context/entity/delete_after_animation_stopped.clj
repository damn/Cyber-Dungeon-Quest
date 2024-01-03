(ns context.entity.delete-after-animation-stopped
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [context.entity :as entity]))

(defcomponent :delete-after-animation-stopped? _
  (entity/create! [_ e _ctx]
    (-> @e :entity/animation :looping? not assert))
  (entity/tick! [_ e _ctx]
    (when (-> @e :entity/animation animation/stopped?)
      (swap! e assoc :destroyed? true))))
