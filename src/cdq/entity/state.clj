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
  (entity/create [[k _] entity* ctx]
    [(assoc entity* k
            ; if :state = nil in fsm => set to initial-state
            ; TODO make PR / bug report.
            {:fsm (assoc (fsm initial-state nil)  ; throws when initial-state is not part of states
                         :state initial-state)
             :state-obj ((initial-state state-obj-constructors) ctx entity*)
             :state-obj-constructors state-obj-constructors})])

  (entity/tick         [_ entity* ctx]         (state/tick state-obj entity* ctx))
  (entity/render-below [_ entity* ctx] (state/render-below state-obj entity* ctx))
  (entity/render-above [_ entity* ctx] (state/render-above state-obj entity* ctx))
  (entity/render-info  [_ entity* ctx] (state/render-info  state-obj entity* ctx)))

; TODO => don't save entity in constructor
; => pass entity* always
; => easier !
; => pass entity* in the first place.
; => cells fetchers could also be entity*'s ??? idk.

; active skill constructor needs entity* for setting action counter
; => potential-field-follow-to-enemy needs entity reference ...
; => also npc idle / player idle for source-entity need to use the ref... ?

; TODO maybe pass entity* here, no need to keep in record?

(extend-type gdl.context.Context
  cdq.context/FiniteStateMachine
  (send-event!
    ([ctx entity* event]
     (cdq.context/send-event! ctx entity* event nil))

    ([ctx entity* event params]
     (when-let [{:keys [fsm
                        state-obj
                        state-obj-constructors]} (:entity/state entity*)]
       (let [old-state (:state fsm)
             new-fsm (fsm/fsm-event fsm event)
             new-state (:state new-fsm)]
         (when (not= old-state new-state)
           (let [constructor (new-state state-obj-constructors)
                 new-state-obj (if params
                                 (constructor ctx entity* params)
                                 (constructor ctx entity*))]
             (transact-all! ctx (state/exit      state-obj entity* ctx))
             (transact-all! ctx (state/enter new-state-obj @(entity/reference entity*) ctx))
             (when (:entity/player? entity*)
               (transact-all! ctx (state/player-enter new-state-obj)))
             (transact-all! ctx
                            [(update @(entity/reference entity*)
                                     :entity/state #(assoc %
                                                           :fsm new-fsm
                                                           :state-obj new-state-obj))]))))))))

(extend-type cdq.entity.Entity
  cdq.entity/State
  (state [entity*]
    (-> entity* :entity/state :fsm :state)))

(defmethod cdq.context/transact! :tx/event [[_ & params] ctx]
  (apply cdq.context/send-event! ctx params)
  nil)
