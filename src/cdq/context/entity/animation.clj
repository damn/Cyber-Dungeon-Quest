(ns cdq.context.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.context.ecs :as ecs]))

(defn- assoc-image-current-frame [entity*]
  (->> entity*
       :entity/animation
       animation/current-frame
       (assoc entity* :entity/image)))

(defcomponent :entity/animation _
  (ecs/create! [_ entity _ctx]
    (swap! entity assoc-image-current-frame))
  (ecs/tick [[k _] entity* {:keys [context/delta-time]}]
    (-> entity*
        (update k animation/tick delta-time)
        assoc-image-current-frame)))
