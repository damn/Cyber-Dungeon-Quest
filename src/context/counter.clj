(ns context.counter
  (:require gdl.context
            game.context))

(defrecord ImmutableCounter [duration stop-time])

(extend-type gdl.context.Context
  game.context/Counter
  (->counter [{:keys [context/elapsed-game-time]} duration]
    {:pre [(>= duration 0)]}
    (->ImmutableCounter duration
                        @elapsed-game-time
                        (+ @elapsed-game-time duration)))

  (stopped? [{:keys [context/elapsed-game-time]}
             {:keys [stop-time]}]
    (>= @elapsed-game-time stop-time))

  (reset [{:keys [context/elapsed-game-time]}
          {:keys [duration]}]
    (assoc counter :stop-time (+ @elapsed-game-time duration)))

  (finished-ratio [{:keys [context/elapsed-game-time] :as context}
                   {:keys [duration stop-time]}]
    {:post [(<= 0 % 1)]}
    (if (game.context/stopped? context counter)
      1
      (/ (- stop-time @elapsed-game-time)
         duration)))

  (update-elapsed-game-time [{:keys [context/elapsed-game-time]}
                             delta]
    (swap! elapsed-game-time + delta)))

(defn ->context []
  {:context/elapsed-game-time (atom 0)})
