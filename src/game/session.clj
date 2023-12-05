(ns game.session)

(defprotocol State
  (load! [_ data])
  (serialize [_])
  (initial-data [_]))
