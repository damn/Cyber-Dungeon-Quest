(nsx game.components.body.rotation-angle)

(defcomponent :rotation-angle _
  (moved [c direction-vector]
    (v/get-angle-from-vector direction-vector)))
