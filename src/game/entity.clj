(ns game.entity)

(defprotocol State
  (enter [_ context])
  (exit  [_ context])
  (tick  [_ delta])
  (tick! [_ context delta])
  (render-below [_ context entity*])
  (render-above [_ context entity*])
  (render-info  [_ context entity*])
  ; offer also debug / effect ?
  )

(defprotocol PlayerState
  (pause-game? [_])
  (manual-tick! [_ context delta]))

; this information 'fsm' should be local to components/state
; => should extend entity record ( in ecs ) with entity protocol mentioned here
(defn get-state [entity*]
  (-> entity* :entity/state :fsm :state))
