(ns game.components.shout
  (:require [x.x :refer [defcomponent]]
            [data.counter :as counter]
            [context.ecs :as entity]
            [game.context :refer [in-line-of-sight? circle->touched-entities send-event!]]))

(def ^:private shout-range 6)

; TODO gets itself also
  ; == faction/friendly? e1 e2 ( entity*/friendly? e*1 e*2) ?
(defn- get-friendly-entities-in-line-of-sight [context entity* radius]
  (->> {:position (:position entity*)
        :radius radius}
       (circle->touched-entities context)
       (filter #(and (= (:faction @%) (:faction entity*))
                     (in-line-of-sight? context entity* @%)))))

(defcomponent :shout counter
  (entity/tick [_ delta]
    (counter/tick counter delta))
  (entity/tick! [_ context entity delta]
    (when (counter/stopped? counter)
      (swap! entity assoc :destroyed? true)
      (doseq [entity (get-friendly-entities-in-line-of-sight context @entity shout-range)]
        (send-event! context entity :alert)))))
