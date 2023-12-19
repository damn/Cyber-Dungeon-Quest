(ns screens.game
  (:require [gdl.context :refer [render-world-view render-gui-view]]
            [gdl.disposable :refer [dispose]]
            gdl.screen
            [game.context :refer [set-screen-stage remove-screen-stage draw act render-world-map
                                    render-in-world-view render-in-gui-view tick-game]]))

(defrecord Screen [stage]
  gdl.disposable/Disposable
  (dispose [_]
    (dispose stage))

  gdl.screen/Screen
  (show [_ context]
    (set-screen-stage context stage))

  (hide [_ context]
    (remove-screen-stage context))

  (render [_ context]
    (render-world-map  context)
    (render-world-view context render-in-world-view)
    (render-gui-view   context render-in-gui-view)
    (draw stage))

  (tick [_ context delta]
    (tick-game context stage delta)
    (act stage delta)))
