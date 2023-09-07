(ns game.components.position
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [int-posi]]
            [game.db :as db]
            [game.components.body :as body]
            [game.maps.contentfields :refer (put-entity-in-correct-content-field
                                             remove-entity-from-content-field)]))

(defcomponent :position p
  (db/create!  [_ e] (put-entity-in-correct-content-field e))
  (db/destroy! [_ e] (remove-entity-from-content-field    e))
  (body/moved! [_ e] (put-entity-in-correct-content-field e)))

(def get-tile (comp int-posi :position))
