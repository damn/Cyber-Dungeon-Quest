(ns cdq.entity.state
  (:require [reduce-fsm :as fsm]
            [x.x :refer [defcomponent]]
            gdl.context
            [cdq.entity :as entity]
            [cdq.context :refer [transact-all!]]))

; TODO states do not have to depend on this ns?
; cdq/state/*
; cdq/state.clj
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

(defcomponent :entity/state {:keys [initial-state
                                    fsm
                                    state-obj
                                    state-obj-constructors]}
  (entity/create [[k _] entity* context]
    [(assoc entity* k
            ; if :state = nil in fsm => set to initial-state
            ; TODO make PR / bug report.
            {:fsm (assoc (fsm initial-state nil)  ; throws when initial-state is not part of states
                         :state initial-state)
             :state-obj ((initial-state state-obj-constructors) context (entity/reference entity*))
             :state-obj-constructors state-obj-constructors})])

  (entity/tick [_ _entity* context]
    (tick state-obj context))

  (entity/render-below [_ entity* c] (render-below state-obj c entity*))
  (entity/render-above [_ entity* c] (render-above state-obj c entity*))
  (entity/render-info  [_ entity* c] (render-info  state-obj c entity*)))

(extend-type gdl.context.Context
  cdq.context/FiniteStateMachine
  (send-event!
    ([ctx entity event]
     (cdq.context/send-event! ctx entity event nil))

    ([ctx entity event params]
     ; 'when' because e.g. sending events to projectiles at wakeup (same faction filter)
     ; who do not have a state component
     (when-let [{:keys [fsm
                        state-obj
                        state-obj-constructors]} (:entity/state @entity)]
       (let [old-state (:state fsm)
             new-fsm (fsm/fsm-event fsm event)
             new-state (:state new-fsm)]
         (when (not= old-state new-state)
           (let [constructor (new-state state-obj-constructors)
                 new-state-obj (if params
                                 (constructor ctx entity params)
                                 (constructor ctx entity))]
             ; TODO maybe pass entity* here, no need to keep in record?
             (transact-all! ctx (exit state-obj ctx))
             (transact-all! ctx (enter new-state-obj ctx))
             (when (:entity/player? @entity)
               (transact-all! ctx (player-enter new-state-obj)))
             (transact-all! ctx
                            [(update @entity :entity/state #(assoc %
                                                                   :fsm new-fsm
                                                                   :state-obj new-state-obj))]))))))))

; TODO can make coll of transactions
; if each @entity references the original original
; and does the stuff only when called
; so only the last one ?
; => nicht machen, sondern beschreiben was machen
; => :db/add

(extend-type cdq.entity.Entity
  cdq.entity/State
  (state [entity*]
    (-> entity* :entity/state :fsm :state)))

(defmethod cdq.context/transact! :tx/event [[_ & params] ctx]
  (apply cdq.context/send-event! ctx params))
