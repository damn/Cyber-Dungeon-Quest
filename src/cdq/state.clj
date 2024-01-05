(ns cdq.state)

(defprotocol State
  (enter [_ ctx])
  (exit  [_ ctx])
  (tick  [_ ctx])
  (render-below [_ ctx entity*])
  (render-above [_ ctx entity*])
  (render-info  [_ ctx entity*]))

(defprotocol PlayerState
  (player-enter [_])
  (pause-game? [_])
  (manual-tick [_ ctx]))

