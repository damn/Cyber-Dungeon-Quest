(ns context.entity.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [context.ecs :as entity]))

(defn- assoc-frame! [e]
  (swap! e #(assoc % :image (animation/current-frame (:animation %)))))

(defcomponent :animation animation
  (entity/create! [_ e _ctx]
    (assoc-frame! e))
  (entity/tick! [_ e _ctx _delta]
    (assoc-frame! e))
  (entity/tick [_ delta]
    (animation/tick animation delta)))
