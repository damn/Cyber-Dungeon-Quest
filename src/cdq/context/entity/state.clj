(ns cdq.context.entity.state
  (:require [reduce-fsm :as fsm]
            [x.x :refer [defcomponent]]
            gdl.context
            [cdq.context.ecs :as ecs]
            cdq.context
            cdq.entity))

(defprotocol State
  (enter [_ context])
  (exit  [_ context])
  (tick  [_ context])
  (render-below [_ context entity*])
  (render-above [_ context entity*])
  (render-info  [_ context entity*]))

(defprotocol PlayerState
  (player-enter [_ context])
  (pause-game? [_])
  (manual-tick! [_ context]))

(defcomponent :entity/state {:keys [initial-state
                                    fsm
                                    state-obj
                                    state-obj-constructors]}
  (ecs/create! [[k _] entity context]
    (swap! entity assoc k
           ; if :state = nil in fsm => set to initial-state
           ; TODO make PR / bug report.
           {:fsm (assoc (fsm initial-state nil)  ; throws when initial-state is not part of states
                        :state initial-state)
            :state-obj ((initial-state state-obj-constructors) context entity)
            :state-obj-constructors state-obj-constructors}))

  (ecs/tick [_ _entity* context]
    (tick state-obj context))

  (ecs/render-below [_ entity* c] (render-below state-obj c entity*))
  (ecs/render-above [_ entity* c] (render-above state-obj c entity*))
  (ecs/render-info  [_ entity* c] (render-info  state-obj c entity*)))

(extend-type gdl.context.Context
  cdq.context/FiniteStateMachine
  (send-event!
    ([context entity event]
     (cdq.context/send-event! context entity event nil))

    ([context entity event params]
     ; 'when' because e.g. sending events to projectiles at wakeup (same faction filter)
     ; who do not have a state component
     (when-let [{:keys [fsm
                        state-obj
                        state-obj-constructors]} (:entity/state @entity)]
       (let [old-state (:state fsm)
             new-fsm (fsm/fsm-event fsm event)
             new-state (:state new-fsm)]
         (when (not= old-state new-state)
           (exit state-obj context)
           (let [constructor (new-state state-obj-constructors)
                 new-state-obj (if params
                                 (constructor context entity params)
                                 (constructor context entity))]
             (enter new-state-obj context)
             (when (:entity/player? @entity)
               (player-enter new-state-obj context))
             (swap! entity update :entity/state #(assoc %
                                                        :fsm new-fsm
                                                        :state-obj new-state-obj)))))))))

(extend-type cdq.entity.Entity
  cdq.entity/State
  (state [entity*]
    (-> entity* :entity/state :fsm :state)))

(defmethod cdq.context.ecs/handle-ctx-transaction!
  :ctx/event
  [[_ & params] ctx]
  (apply cdq.context/send-event! ctx params))
