(ns cdq.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.entity :as entity]))

(defn- tx-assoc-image-current-frame [{:keys [entity/id entity/animation]}]
  [:tx/assoc id :entity/image (animation/current-frame animation)])

(defcomponent :entity/animation animation
  (entity/create [_ entity* _ctx]
    [(tx-assoc-image-current-frame entity*)])
  (entity/tick [[k _] {:keys [entity/id] :as entity*} {:keys [context/delta-time]}]
    [(tx-assoc-image-current-frame entity*)
     [:tx/assoc id k (animation/tick animation delta-time)]]))
