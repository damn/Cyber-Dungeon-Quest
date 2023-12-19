(ns game.components.state.player-dead
  (:require [gdl.app :as app]
            [gdl.context :refer [play-sound!]]
            [game.context :as gm]
            [app.state :refer [change-screen!]]
            [game.components.state :as state])
  (:import (com.badlogic.gdx Gdx Input$Keys)))

(defrecord State [entity]
  state/PlayerState
  (pause-game? [_] true)

  (manual-tick! [_ context delta]
    ; TODO do at end of frame, dont change here, otherwise :context/current-screen is different in
    ; argument context and atom context
    (when (.isKeyJustPressed Gdx/input Input$Keys/X)
      (change-screen! :screens/main-menu)))

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
