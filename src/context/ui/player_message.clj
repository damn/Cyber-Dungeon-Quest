(ns context.ui.player-message
  (:require [gdl.context :refer [draw-text ->actor delta-time]]))

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

(extend-type gdl.context.Context
  cdq.context/PlayerMessage
  (show-msg-to-player! [{:keys [context/player-message] :as context}
                        message]
    (reset! player-message {:message message :counter 0}))

  (->player-message-actor [context]
    (->actor context
             {:draw draw-player-message
              :act check-remove-message})))

(defn ->context []
  {:context/player-message (atom nil)})
