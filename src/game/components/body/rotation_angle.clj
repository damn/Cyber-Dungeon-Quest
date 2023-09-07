(ns game.components.body.rotation-angle
  (:require [x.x :refer [defcomponent]]
            [gdl.vector :as v]
            [game.components.body :as body]))

(defcomponent :rotation-angle _
  (body/moved [c direction-vector]
    (v/get-angle-from-vector direction-vector)))
