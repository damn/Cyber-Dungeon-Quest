(ns cdq.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [cdq.entity :as entity]))

(defn- assoc-image-current-frame [entity*]
  (->> entity*
       :entity/animation
       animation/current-frame
       (assoc entity* :entity/image)))

(defcomponent :entity/animation _
  (entity/create! [_ entity _ctx]
    (swap! entity assoc-image-current-frame))
  (entity/tick [[k _] entity* {:keys [context/delta-time]}]
    [(-> entity*
         (update k animation/tick delta-time)
         assoc-image-current-frame)]))
