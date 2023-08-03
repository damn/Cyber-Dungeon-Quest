(nsx game.screens.load-session
  (:require game.player.session-data))

(def is-loaded-character (atom false))

(def ^:private render-once (atom false))

; TODO not showing at all
(game/defscreen loading-screen
  :show (fn []
          (reset! render-once false))
  :render (fn []
            (g/render-gui
             (fn []
               (reset! render-once true)
               #_(font/draw-text "Loading..."
                               (/ (g/viewport-width) 2)
                               (/ (g/viewport-height) 2)
                               #_{:centerx true}
                               ))))
  :update (fn [delta]
            (when @render-once
              ;(log "Loading new session")
              (game.player.session-data/init @is-loaded-character)
              ;(log "Finished loading new session")
              (game/set-screen :ingame))))

