(ns game.faction)

(defn enemy [faction]
  (case faction
    :evil :good
    :good :evil))

; => TODO move to game.entity ?
; & fix potential-field pass faction there directly dont depend on faction
