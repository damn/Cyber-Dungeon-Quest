(ns game.components.body.rotation-angle
  (:require [x.x :refer [defcomponent]]
            [gdl.vector :as v]
            [game.systems :refer [moved]]))

(defcomponent :rotation-angle _
  (moved [c direction-vector]
    (v/get-angle-from-vector direction-vector)))
