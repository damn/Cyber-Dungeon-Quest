(ns game.components.animation
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [game.tick :refer [tick tick!]]
            [game.db :as db]))

(defn- assoc-frame! [r]
  (swap! r #(assoc % :image (animation/get-frame (:animation %)))))

(defcomponent :animation animation
  (db/create! [_ e]
    (assoc-frame! e))
  (tick! [c e _delta]
    (assoc-frame! e))
  (tick [_c delta]
    (animation/update animation delta)))
