(ns game.components.state.player-dead
  (:require [gdl.app :as app]
            [game.protocols :as gm]
            [game.components.state :as state])
  (:import (com.badlogic.gdx Gdx Input$Keys)))

(defrecord State [entity]
  state/PlayerState
  (pause-game? [_] true)
  (manual-tick! [_ context delta]
    (when (.isKeyJustPressed Gdx/input Input$Keys/X)
      (app/change-screen! :screens/main-menu)))

  state/State
  (enter [_ context]
    (gm/play-sound! context "sounds/bfxr_playerdeath.wav")
    (gm/show-msg-to-player! context "YOU DIED!. Press X to leave."))
  (exit [_ _ctx])
  (tick [this delta] this)
  (tick! [_ _ctx delta])
  (render-below [_ drawer context entity*])
  (render-above [_ drawer context entity*])
  (render-info  [_ drawer context entity*]))
