(ns game.components.faction)

; TODO ALSO PROTOCOL !

(defn enemy [faction]
  (case faction
    :evil :good
    :good :evil))
