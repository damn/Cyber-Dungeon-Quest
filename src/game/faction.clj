(ns game.faction)

(defn enemy [faction]
  (case faction
    :evil :good
    :good :evil))
