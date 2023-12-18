(ns screens.game
  (:require [gdl.protocols :refer [dispose
                                   render-world-view
                                   render-gui-view]]
            gdl.screen
            [game.protocols :refer [set-screen-stage
                                    remove-screen-stage
                                    draw
                                    act
                                    render-world-map
                                    render-in-world-view
                                    render-in-gui-view
                                    tick-game]]))

(defrecord Screen [stage]
  gdl.protocols/Disposable
  (dispose [_]
    (dispose stage))

  gdl.screen/Screen
  (show [_ context]
    (set-screen-stage context stage))

  (hide [_ context]
    (remove-screen-stage context))

  (render [_ context]
    (render-world-map context)
    (render-world-view context (partial render-in-world-view context))
    (render-gui-view   context (partial render-in-gui-view   context))
    (draw stage))

  (tick [_ context delta]
    (tick-game context stage delta)
    (act stage delta)))
