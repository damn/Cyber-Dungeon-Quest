(ns cdq.context.ui.player-message
  (:require [gdl.context :refer [draw-text ->actor delta-time]]
            cdq.api.context))

(def ^:private duration-seconds 1.5)

(defn- draw-player-message
  [{:keys [context/player-message
           gui-viewport-width
           gui-viewport-height] :as context}]
  (when-let [{:keys [message]} @player-message]
    (draw-text context
               {:x (/ gui-viewport-width  2)
                :y (+ (/ gui-viewport-height 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message
  [{:keys [context/player-message] :as context}]
  (when-let [{:keys [counter]} @player-message]
    (swap! player-message update :counter + (delta-time context))
    (when (>= counter duration-seconds)
      (reset! player-message nil))))

(defmethod cdq.api.context/transact! :tx/msg-to-player [[_ message]
                                                    {:keys [context/player-message]}]
  (reset! player-message {:message message :counter 0})
  nil)

(defn ->player-message-actor [ctx]
  (->actor ctx
           {:draw draw-player-message
            :act check-remove-message}))

(defn ->context []
  {:context/player-message (atom nil)})
