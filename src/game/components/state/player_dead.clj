(ns game.components.state.player-dead
  (:require [gdl.context :refer [play-sound!]]
            [game.context :as gm]
            [game.components.state :as state]))

(defrecord State [entity]
  state/PlayerState
  (pause-game? [_] true)

  (manual-tick! [_ context delta])

  state/State
  (enter [_ context]
    (play-sound! context "sounds/bfxr_playerdeath.wav")
    (gm/show-msg-to-player! context "YOU DIED!. Press X to leave."))

  (exit [_ _ctx])
  (tick [this delta] this)
  (tick! [_ _ctx delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
