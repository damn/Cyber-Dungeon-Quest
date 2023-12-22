(ns game.entity)

(defprotocol State
  (state [_]))

(defprotocol Skills
  (has-skill? [_ skill]))
