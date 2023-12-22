(ns context.ui.player-message
  (:require [gdl.context :refer [draw-text]]
            [data.counter :as counter]
            [app.state :refer [current-context]]
            game.context)
  (:import com.badlogic.gdx.scenes.scene2d.Actor))

(defn- draw-player-message
  [{:keys [context/player-message
           gui-viewport-width
           gui-viewport-height] :as context}]
  (when-let [{:keys [message]} @player-message]
    (draw-text (update context :unit-scale * 2.5)
               {:x (/ gui-viewport-width  2)
                :y (+ (/ gui-viewport-height 2) 200)
                :text message})))

(defn- update-check-counter
  [{:keys [context/player-message]} delta]
  (when-let [{:keys [counter]} @player-message]
    (swap! player-message update :counter counter/tick delta)
    (when (counter/stopped? counter)
      (reset! player-message nil))))

; TODO player message triggers on enter game screen already => invalid skill params
; => check when triggered or why (key just pressed ?)
; TODO when you died, keep message there , add duration param

(extend-type gdl.context.Context
  game.context/PlayerMessage
  (show-msg-to-player! [{:keys [context/player-message]}
                        message]
    (reset! player-message {:message message
                            :counter (counter/create 3)})) ; stage gets updated in seconds

  (->player-message-actor [_]
    (proxy [Actor] []
      (draw [_batch _parent-alpha]
        (draw-player-message @current-context))
      (act [delta]
        (update-check-counter @current-context delta)))))

(defn ->context []
  {:context/player-message (atom nil)})
