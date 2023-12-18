(ns screens.game
  (:require [gdl.lifecycle :as lc :refer [dispose]]
            [game.protocols :refer [render-view
                                    set-screen-stage
                                    remove-screen-stage
                                    draw
                                    act
                                    render-world-map
                                    render-in-world-view
                                    render-in-gui-view
                                    tick-game
                                    create-gui-stage]]))

(defrecord Screen [stage]
  lc/Disposable
  (dispose [_]
    (dispose stage))

  lc/Screen
  (show [_ context]
    (set-screen-stage context stage))

  (hide [_ context]
    (remove-screen-stage context))

  (render [_ context]
    (render-world-map context)
    (render-view context :world (partial render-in-world-view context))
    (render-view context :gui   (partial render-in-gui-view   context))
    (draw stage))

  (tick [_ context delta]
    (tick-game context stage delta)
    (act stage delta)))

(defn screen [context actors]
  (->Screen (create-gui-stage context actors)))
