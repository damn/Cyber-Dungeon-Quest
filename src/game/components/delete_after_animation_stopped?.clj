(ns game.components.delete-after-animation-stopped?
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.animation :as animation]
            [game.tick :refer [tick!]]))

(defcomponent :delete-after-animation-stopped? _
  (tick! [_ e delta]
    (when (-> @e :animation animation/stopped?)
      (swap! e assoc :destroyed? true))))
