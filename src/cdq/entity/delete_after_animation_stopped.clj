(ns cdq.entity.delete-after-animation-stopped
  (:require [core.component :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.api.entity :as entity]))

(defcomponent :entity/delete-after-animation-stopped? {}
  _
  (entity/create [_ entity* _ctx]
    (-> entity* :entity/animation :looping? not assert))
  (entity/tick [_ {:keys [entity/id entity/animation]} _ctx]
    (when (animation/stopped? animation)
      [[:tx/destroy id]])))
