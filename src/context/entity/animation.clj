(ns context.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [context.entity :as entity]))

(defn- assoc-frame! [e]
  (swap! e #(assoc % :image (animation/current-frame (:animation %)))))

(defcomponent :animation animation
  (entity/create! [_ e _ctx]
    (assoc-frame! e))
  (entity/tick! [[k _] e _ctx delta]
    (assoc-frame! e)
    (swap! e update k animation/tick delta)))
