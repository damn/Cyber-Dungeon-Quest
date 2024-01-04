(ns cdq.entity.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-text pixels->world-units]]
            [cdq.context.ui.config :refer [hpbar-height-px]]
            [cdq.entity :as entity]
            [cdq.context :refer [->counter stopped? reset]]))

(defcomponent :entity/string-effect {:keys [text counter] :as this}
  (entity/tick [[k _] entity* context]
    (when (stopped? context counter)
      [(dissoc entity* k)]))

  (entity/render-above [_ {[x y] :entity/position :keys [entity/body]} c]
    (draw-text c
               {:text text
                :x x
                :y (+ y (:half-height body) (pixels->world-units c hpbar-height-px))
                :scale 2
                :up? true})))

(extend-type gdl.context.Context
  cdq.context/TextEffect
  (add-text-effect! [context entity text]
    (if (:entity/string-effect @entity)
      (swap! entity update :entity/string-effect #(-> %
                                                      (update :text str "\n" text)
                                                      (update :counter (fn [cnt]
                                                                         (reset context cnt)))))
      (swap! entity assoc :entity/string-effect {:text text
                                                 :counter (->counter context 0.4)}))))
