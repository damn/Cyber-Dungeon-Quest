(ns cdq.entity.shout
  (:require [x.x :refer [defcomponent]]
            [cdq.entity :as entity]
            [cdq.context :refer [world-grid line-of-sight? stopped?]]
            [cdq.world.grid :refer [circle->entities]]))

(def ^:private shout-range 6)

; TODO gets itself also
  ; == faction/friendly? e1 e2 ( entity*/friendly? e*1 e*2) ?
(defn- get-friendly-entities-in-line-of-sight [context entity* radius]
  (->> {:position (:entity/position entity*)
        :radius radius}
       (circle->entities (world-grid context))
       (filter #(and (= (:entity/faction @%) (:entity/faction entity*))
                     (line-of-sight? context entity* @%)))))

(defcomponent :entity/shout counter
  (entity/tick [_ entity* context]
    (when (stopped? context counter)
      (cons (assoc entity* :entity/destroyed? true)
            (for [entity (get-friendly-entities-in-line-of-sight context entity* shout-range)]
              [:tx/event entity :alert])))))
