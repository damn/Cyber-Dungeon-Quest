(ns cdq.entity.state.player-dead
  (:require [gdl.app :refer [change-screen!]]
            [cdq.entity.state :as state]))

(defrecord PlayerDead [entity]
  state/PlayerState
  (player-enter [_]
    [[:tx/cursor :cursors/black-x]])

  (pause-game? [_] true)
  (manual-tick [_ context])

  state/State
  (enter [_ _ctx]
    [[:tx/sound  "sounds/bfxr_playerdeath.wav"]
     [:tx/player-modal {:title "YOU DIED"
                        :text "\nGood luck next time"
                        :button-text ":("
                        :on-click (fn [_ctx]
                                    (change-screen! :screens/main-menu))}]])
  (exit [_ _ctx])
  (tick [_ _ctx])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
