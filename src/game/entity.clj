(ns game.entity)

; this information 'fsm' should be local to components/state
; => should extend entity record ( in ecs ) with entity protocol mentioned here
(defn state [entity*]
  (-> entity* :entity/state :fsm :state))
