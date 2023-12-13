; TODO delete, make window modal
(ns game.screens.load-session
  (:require [x.x :refer [defmodule]]
            [gdl.lifecycle :as lc]
            [gdl.app :as app]
            game.player.session-data))

(def is-loaded-character (atom false))

(def ^:private render-once (atom false))

; TODO not showing at all -> render two frames ? check
(defmodule _
  (lc/show [_ _ctx]
    (reset! render-once false))
  (lc/render [_ {:keys [batch]}]
    (reset! render-once true)
    #_(app/render-with :gui
                (fn [_unit-scale]
                  (reset! render-once true)
                  #_(font/draw-text "Loading..."
                                    (/ gui-viewport-width 2)
                                    (/ gui-viewport-height 2)
                                    #_{:centerx true}
                                    ))))
  (lc/tick [_ _state delta]
    (when @render-once
      ;(log "Loading new session")
      (game.player.session-data/init @is-loaded-character)
      ;(log "Finished loading new session")
      (app/set-screen :game.screens.ingame))))

