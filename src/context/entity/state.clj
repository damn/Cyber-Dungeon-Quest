(ns context.entity.state
  (:require [reduce-fsm :as fsm]
            [x.x :refer [defcomponent]]
            gdl.context
            game.context
            [context.entity :as ecs]
            [game.entity :as entity]))

(defcomponent :entity/state {:keys [initial-state
                                    fsm
                                    state-obj
                                    state-obj-constructors]}
  (ecs/create! [_ entity _ctx]
    (swap! entity assoc :entity/state
           {:fsm (fsm initial-state nil) ; throws when initial-state is not part of states
            :state-obj ((initial-state state-obj-constructors) entity)
            :state-obj-constructors state-obj-constructors}))
  (ecs/tick [[_ v] delta]
    (update v :state-obj entity/tick delta))
  (ecs/tick! [_ _entity context delta]
    (entity/tick! state-obj context delta))
  (ecs/render-below [_ entity* c] (entity/render-below state-obj c entity*))
  (ecs/render-above [_ entity* c] (entity/render-above state-obj c entity*))
  (ecs/render-info  [_ entity* c] (entity/render-info  state-obj c entity*)))

(extend-type gdl.context.Context
  game.context/FiniteStateMachine
  (send-event!
    ([context entity event]
     (game.context/send-event! context entity event nil))

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
           (entity/exit state-obj context)
           (let [constructor (new-state state-obj-constructors)
                 new-state-obj (if params
                                 (constructor entity params)
                                 (constructor entity))]
             (entity/enter new-state-obj context)
             (swap! entity update :entity/state #(assoc %
                                                        :fsm new-fsm
                                                        :state-obj new-state-obj)))))))))
