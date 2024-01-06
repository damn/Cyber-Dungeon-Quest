(ns cdq.state.player-found-princess
  (:require [gdl.app :refer [change-screen!]]
            [cdq.state :as state]))

(defrecord PlayerFoundPrincess []
  state/PlayerState
  (player-enter [_] [[:tx/cursor :cursors/black-x]])
  (pause-game? [_] true)
  (manual-tick [_ entity* context])
  (clicked-inventory-cell [_ entity* cell])
  (clicked-skillmenu-skill [_ entity* skill])

  state/State
  (enter [_ _entity* _ctx]
    [[:tx/sound "sounds/bfxr_playerdeath.wav"]
     [:tx/player-modal {:title "YOU WON!"
                        :text "\nYou found the princess!"
                        :button-text ":)"
                        :on-click (fn [_ctx]
                                    (change-screen! :screens/main-menu))}]])
  (exit [_ entity* _ctx])
  (tick [_ entity* _ctx])
  (render-below [_ entity* c])
  (render-above [_ entity* c])
  (render-info  [_ entity* c]))
