(ns cdq.entity.state
  (:require [reduce-fsm :as fsm]
            [x.x :refer [defcomponent]]
            gdl.context
            [cdq.context :refer [transact-all!]]
            [cdq.entity :as entity]
            [cdq.state :as state]))

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
    (state/tick state-obj context))

  (entity/render-below [_ entity* c] (state/render-below state-obj c entity*))
  (entity/render-above [_ entity* c] (state/render-above state-obj c entity*))
  (entity/render-info  [_ entity* c] (state/render-info  state-obj c entity*)))

; TODO => don't save entity in constructor
; => pass entity* always
; => easier !
; => pass entity* in the first place.
; -> can remove source*/target*
; => cells fetchers could also be entity*'s ??? idk.

(extend-type gdl.context.Context
  cdq.context/FiniteStateMachine
  (send-event!
    ([ctx entity event]
     (cdq.context/send-event! ctx entity event nil))

    ([ctx entity event params]
     (let [entity (if (map? entity) (entity/reference entity) entity)]
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
               (transact-all! ctx (state/exit state-obj ctx))
               (transact-all! ctx (state/enter new-state-obj ctx))
               (when (:entity/player? @entity)
                 (transact-all! ctx (state/player-enter new-state-obj)))
               (transact-all! ctx
                              [(update @entity :entity/state #(assoc %
                                                                     :fsm new-fsm
                                                                     :state-obj new-state-obj))])))))))))

(extend-type cdq.entity.Entity
  cdq.entity/State
  (state [entity*]
    (-> entity* :entity/state :fsm :state)))

; TODO needs to handle dereffed entity*'s now ! (pass everywhere ?)
(defmethod cdq.context/transact! :tx/event [[_ & params] ctx]
  (apply cdq.context/send-event! ctx params)
  nil)
