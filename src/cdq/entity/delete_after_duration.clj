(ns cdq.entity.delete-after-duration
  (:require [x.x :refer [defcomponent]]
            [cdq.context :refer [->counter stopped?]]
            [cdq.entity :as entity]))

(defcomponent :entity/delete-after-duration counter
  (entity/create [[k duration] {:keys [entity/id]} ctx]
    [[:tx/assoc id k (->counter ctx duration)]])
  (entity/tick [_ {:keys [entity/id]} ctx]
    (when (stopped? ctx counter)
      [[:tx/destroy id]])))
