(ns context.player-message
  (:require gdl.context
            game.context))

(extend-type gdl.context.Context
  game.context/PlayerMessage
  (show-msg-to-player [_ message]
    (println message)))
