(ns cdq.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.entity :as entity]))

(defn- tx-assoc-image-current-frame [entity*]
  [:tx/assoc entity* :entity/image (->> entity*
                                        :entity/animation
                                        animation/current-frame)])

(defcomponent :entity/animation animation
  (entity/create [_ entity* _ctx]
    [(tx-assoc-image-current-frame entity*)])
  (entity/tick [[k _] entity* {:keys [context/delta-time]}]
    [(tx-assoc-image-current-frame entity*)
     [:tx/assoc entity* k (animation/tick animation delta-time)]]))
