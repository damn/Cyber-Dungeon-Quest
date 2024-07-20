(ns cdq.entity.delete-after-duration
  (:require [x.x :refer [defcomponent]]
            [cdq.api.context :refer [->counter stopped?]]
            [cdq.api.entity :as entity]))

(defcomponent :entity/delete-after-duration counter
  (entity/create-component [[_ duration] _components ctx]
    (->counter ctx duration))

  (entity/tick [_ {:keys [entity/id]} ctx]
    (when (stopped? ctx counter)
      [[:tx/destroy id]])))
