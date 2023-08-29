(nsx game.components.delete-after-animation-stopped?
  (:require x.ns)) ; one time require so clojure.tools.namespace knows dependency order
; (in the ns which loads after gdl first at refresn-all ...)

(defcomponent :delete-after-animation-stopped? _
  (tick! [_ e delta]
    (when (-> @e :animation animation/stopped?)
      (swap! e assoc :destroyed? true))))
