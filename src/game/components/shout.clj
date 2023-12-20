(ns game.components.shout
  (:require [x.x :refer [defcomponent]]
            [data.counter :as counter]
            [context.ecs :as entity]
            [game.context :refer [in-line-of-sight?]]
            [game.components.state :as state]
            [game.maps.cell-grid :as cell-grid]))

(def ^:private shout-range 6)

(defn- get-friendly-entities-in-line-of-sight [{:keys [context/world-map]
                                                :as context}
                                               entity*
                                               radius]
  (filter #(and (= (:faction @%) (:faction entity*)
                   (in-line-of-sight? context entity* @%)))
          (cell-grid/circle->touched-entities (:cell-grid world-map)
                                              {:position (:position entity*)
                                               :radius radius})))

(defcomponent :shout counter
  (entity/tick [_ delta]
    (counter/tick counter delta))
  (entity/tick! [_ context entity delta]
    (when (counter/stopped? counter)
      (swap! entity assoc :destroyed? true)
      (doseq [entity (get-friendly-entities-in-line-of-sight context @entity shout-range)]
        (state/send-event! context entity :alert)))))
