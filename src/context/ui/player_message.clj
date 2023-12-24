(ns context.ui.player-message
  (:require [gdl.context :refer [draw-text ->actor]]
            [cdq.context :refer [stopped? ->counter]]))

(defn- draw-player-message
  [{:keys [context/player-message
           gui-viewport-width
           gui-viewport-height] :as context}]
  (when-let [{:keys [message]} @player-message]
    (draw-text context
               {:x (/ gui-viewport-width  2)
                :y (/ gui-viewport-height 2)
                :text message
                :scale 2.5
                :up? true})))

(defn- update-check-counter
  [{:keys [context/player-message] :as context}]
  (when-let [{:keys [counter]} @player-message]
    (when (stopped? context counter)
      (reset! player-message nil))))

; TODO player message triggers on enter game screen already => invalid skill params
; => check when triggered or why (key just pressed ?)
; TODO when you died, keep message there , add duration param

(extend-type gdl.context.Context
  cdq.context/PlayerMessage
  (show-msg-to-player! [{:keys [context/player-message] :as context}
                        message]
    (reset! player-message {:message message
                            :counter (->counter context 3)})) ; stage gets updated in seconds

  (->player-message-actor [context]
    (->actor context
             {:draw draw-player-message
              :act update-check-counter})))

(defn ->context []
  {:context/player-message (atom nil)})