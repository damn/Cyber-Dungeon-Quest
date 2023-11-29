(ns game.components.delete-after-animation-stopped?
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [game.db :as db]
            [game.tick :refer [tick!]]))

(defcomponent :delete-after-animation-stopped? _
  (db/create! [_ e]
    (-> @e :animation :looping not assert))
  (tick! [_ e delta]
    (when (-> @e :animation animation/stopped?)
      (swap! e assoc :destroyed? true))))
