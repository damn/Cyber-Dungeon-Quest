(ns entity.delete-after-animation-stopped?
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [context.ecs :as entity]))

(defcomponent :delete-after-animation-stopped? _
  (entity/create! [_ e _ctx]
    (-> @e :animation :looping? not assert))
  (entity/tick! [_ e _ctx delta]
    (when (-> @e :animation animation/stopped?)
      (swap! e assoc :destroyed? true))))
