(nsx game.components.delete-after-animation-stopped?)

(defcomponent :delete-after-animation-stopped? _
  (tick! [_ e delta]
    (when (-> @e :animation animation/stopped?)
      (swap! e assoc :destroyed? true))))
