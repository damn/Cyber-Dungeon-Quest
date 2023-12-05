(ns game.components.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [game.entity :as entity]))

(defn- assoc-frame! [e]
  (swap! e #(assoc % :image (animation/get-frame (:animation %)))))

(defcomponent :animation animation
  (entity/create! [_ e]
    (assoc-frame! e))
  (entity/tick! [_ e _delta]
    (assoc-frame! e))
  (entity/tick [_ delta]
    (animation/update animation delta)))
