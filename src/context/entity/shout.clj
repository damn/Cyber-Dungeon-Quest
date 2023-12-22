(ns context.entity.shout
  (:require [x.x :refer [defcomponent]]
            [data.counter :as counter]
            [context.ecs :as entity]
            [game.context :refer [world-grid line-of-sight? send-event!]]
            [game.world.grid :refer [circle->entities]]))

(def ^:private shout-range 6)

; TODO gets itself also
  ; == faction/friendly? e1 e2 ( entity*/friendly? e*1 e*2) ?
(defn- get-friendly-entities-in-line-of-sight [context entity* radius]
  (->> {:position (:position entity*)
        :radius radius}
       (circle->entities (world-grid context))
       (filter #(and (= (:faction @%) (:faction entity*))
                     (line-of-sight? context entity* @%)))))

(defcomponent :shout counter
  (entity/tick [_ delta]
    (counter/tick counter delta))
  (entity/tick! [_ entity context delta]
    (when (counter/stopped? counter)
      (swap! entity assoc :destroyed? true)
      (doseq [entity (get-friendly-entities-in-line-of-sight context @entity shout-range)]
        (send-event! context entity :alert)))))
