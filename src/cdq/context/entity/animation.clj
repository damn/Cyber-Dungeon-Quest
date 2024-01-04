(ns cdq.context.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.context.ecs :as ecs]))

(defn- assoc-image-current-frame [entity*]
  (->> entity*
       :entity/animation
       animation/current-frame
       (assoc entity* :entity/image)))

(defcomponent :entity/animation animation
  (ecs/create! [_ entity _ctx]
    (swap! entity assoc-entity-image-current-frame))
  (ecs/tick! [[k _] entity {:keys [context/delta-time]}]
    (swap! entity #(-> %
                       (update k animation/tick delta-time)
                       assoc-entity-image-current-frame))))
