(ns context.entity.state
  (:require [reduce-fsm :as fsm]
            [x.x :refer [defcomponent]]
            gdl.context
            [context.entity :as entity]
            game.context
            game.entity))

(defprotocol State
  (enter [_ context])
  (exit  [_ context])
  (tick! [_ context delta])
  (render-below [_ context entity*])
  (render-above [_ context entity*])
  (render-info  [_ context entity*]))

(defprotocol PlayerState
  (pause-game? [_])
  (manual-tick! [_ context delta]))

(defcomponent :entity/state {:keys [initial-state
                                    fsm
                                    state-obj
                                    state-obj-constructors]}
  (entity/create! [_ entity context]
    (swap! entity assoc :entity/state
           {:fsm (fsm initial-state nil) ; throws when initial-state is not part of states
            :state-obj ((initial-state state-obj-constructors) context entity)
            :state-obj-constructors state-obj-constructors}))

  (entity/tick! [_ _entity context delta]
    (tick! state-obj context delta))

  (entity/render-below [_ entity* c] (render-below state-obj c entity*))
  (entity/render-above [_ entity* c] (render-above state-obj c entity*))
  (entity/render-info  [_ entity* c] (render-info  state-obj c entity*)))

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
           (exit state-obj context)
           (let [constructor (new-state state-obj-constructors)
                 new-state-obj (if params
                                 (constructor context entity params)
                                 (constructor context entity))]
             (enter new-state-obj context)
             (swap! entity update :entity/state #(assoc %
                                                        :fsm new-fsm
                                                        :state-obj new-state-obj)))))))))

(extend-type context.entity.Entity
  game.entity/State
  (state [entity*]
    (-> entity* :entity/state :fsm :state)))
