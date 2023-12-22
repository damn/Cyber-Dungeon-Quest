(ns game.entity)

(defprotocol State
  (state [_]))

(defprotocol Skills
  (add-skill [_ skill])
  (remove-skill [_ skill])
  (has-skill? [_ skill])
  (set-skill-to-cooldown [_ skill]))
