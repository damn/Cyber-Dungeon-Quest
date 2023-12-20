(ns game.entity)

(defprotocol State
  (enter [_ context])
  (exit  [_ context])
  (tick  [_ delta])
  (tick! [_ context delta])
  (render-below [_ context entity*])
  (render-above [_ context entity*])
  (render-info  [_ context entity*])
  ; TODO offer also debug / effect ?
  )

(defprotocol PlayerState
  (pause-game? [_])
  (manual-tick! [_ context delta]))
