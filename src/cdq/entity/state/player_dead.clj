(ns cdq.entity.state.player-dead
  (:require [gdl.app :refer [change-screen!]]
            [gdl.context :refer [play-sound!]]
            [cdq.entity.state :as state]
            [cdq.context :refer [set-cursor! show-player-modal!]]))

(defrecord PlayerDead [entity]
  state/PlayerState
  (player-enter [_ ctx]
    (set-cursor! ctx :cursors/black-x))

  (pause-game? [_] true)
  (manual-tick [_ context])

  state/State
  (enter [_ ctx]
    (play-sound! ctx "sounds/bfxr_playerdeath.wav")
    (show-player-modal! ctx {:title "YOU DIED"
                             :text "\nGood luck next time"
                             :button-text ":("
                             :on-click (fn [_ctx]
                                         (change-screen! :screens/main-menu))}))
  (exit [_ _ctx])
  (tick [_ _ctx])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
