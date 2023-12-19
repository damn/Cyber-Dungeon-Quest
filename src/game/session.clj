(ns game.session
  (:require gdl.context
            game.context))

(extend-type gdl.context.Context
  game.context/Context
  (show-msg-to-player! [_ message]
    (println message)))
