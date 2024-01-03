(ns context.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [context.entity :as entity]))

(defn- assoc-frame! [e]
  (swap! e #(assoc % :entity/image (animation/current-frame (:entity/animation %)))))

(defcomponent :entity/animation animation
  (entity/create! [_ e _ctx]
    (assoc-frame! e))
  (entity/tick! [[k _] e {:keys [context/delta-time]}]
    (assoc-frame! e)
    (swap! e update k animation/tick delta-time)))
