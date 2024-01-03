(ns cdq.context.entity.state.player
  (:require [reduce-fsm :as fsm]
            (cdq.context.entity.state [active-skill :as active-skill]
                                      [player-dead :as player-dead]
                                      [player-found-princess :as player-found-princess]
                                      [player-idle :as player-idle]
                                      [player-item-on-cursor :as player-item-on-cursor]
                                      [player-moving :as player-moving]
                                      [stunned :as stunned])))

(fsm/defsm-inc ^:private player-fsm
  [[:idle
    :kill -> :dead
    :stun -> :stunned
    :start-action -> :active-skill
    :pickup-item -> :item-on-cursor
    :movement-input -> :moving
    :found-princess -> :princess-saved]
   [:moving
    :kill -> :dead
    :stun -> :stunned
    :no-movement-input -> :idle]
   [:active-skill
    :kill -> :dead
    :stun -> :stunned
    :action-done -> :idle]
   [:stunned
    :kill -> :dead
    :effect-wears-off -> :idle]
   [:item-on-cursor
    :kill -> :dead
    :stun -> :stunned
    :drop-item -> :idle
    :dropped-item -> :idle]
   [:princess-saved]
   [:dead]])

(def ^:private player-state-constructors
  {:item-on-cursor (fn [_ctx e item] (player-item-on-cursor/->PlayerItemOnCursor e item))
   :idle           (fn [_ctx e] (player-idle/->PlayerIdle e))
   :moving         (fn [_ctx e v] (player-moving/->PlayerMoving e v))
   :active-skill   active-skill/->CreateWithCounter
   :stunned        stunned/->CreateWithCounter
   :dead           (fn [_ctx e] (player-dead/->PlayerDead e))
   :princess-saved (fn [_ctx e] (player-found-princess/->PlayerFoundPrincess e))})

(defn ->state [initial-state]
  {:initial-state initial-state
   :fsm player-fsm
   :state-obj-constructors player-state-constructors})
