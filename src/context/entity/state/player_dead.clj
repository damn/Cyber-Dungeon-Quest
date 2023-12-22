(ns context.entity.state.player-dead
  (:require [gdl.context :refer [play-sound!]]
            [game.context :refer [show-msg-to-player!]]
            [context.entity.state :as state]))

(defrecord PlayerDead [entity]
  state/PlayerState
  (pause-game? [_] true)
  (manual-tick! [_ context delta])

  state/State
  (enter [_ context]
    (play-sound! context "sounds/bfxr_playerdeath.wav")
    (show-msg-to-player! context "YOU DIED!. Press X to leave."))
  (exit [_ _ctx])
  (tick! [_ _ctx delta])
  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info  [_ c entity*]))
