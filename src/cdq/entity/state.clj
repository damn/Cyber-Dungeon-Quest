(ns cdq.entity.state
  (:require [reduce-fsm :as fsm]
            [x.x :refer [defcomponent]]
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

(defn- send-event! [ctx entity event params]
  (when-let [{:keys [fsm
                     state-obj
                     state-obj-constructors]} (:entity/state @entity)]
    (let [old-state (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state (:state new-fsm)]
      (when (not= old-state new-state)
        (let [constructor (new-state state-obj-constructors)
              new-state-obj (if params
                              (constructor ctx @entity params)
                              (constructor ctx @entity))]
          (transact-all! ctx (state/exit      state-obj @entity ctx))
          (transact-all! ctx (state/enter new-state-obj @entity ctx))
          (when (:entity/player? @entity)
            (transact-all! ctx (state/player-enter new-state-obj)))
          (transact-all! ctx
                         [(update @entity
                                  :entity/state
                                  #(assoc % :fsm new-fsm :state-obj new-state-obj))]))))))

(extend-type cdq.entity.Entity
  cdq.entity/State
  (state [entity*]
    (-> entity* :entity/state :fsm :state))

  (state-obj [entity*]
    (-> entity* :entity/state :state-obj)))

(defmethod cdq.context/transact! :tx/event [[_ entity* event params] ctx]
  (send-event! ctx (entity/reference entity*) event params)
  nil)
