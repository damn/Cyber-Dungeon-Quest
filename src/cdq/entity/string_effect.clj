(ns cdq.entity.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-text pixels->world-units]]
            [cdq.context :refer [->counter stopped? reset]]
            [cdq.context.ui.config :refer [hpbar-height-px]]
            [cdq.entity :as entity]))

(defcomponent :entity/string-effect {:keys [text counter] :as this}
  (entity/tick [[k _] {:keys [entity/id]} context]
    (when (stopped? context counter)
      [[:tx/dissoc id k]]))

  (entity/render-above [_ {[x y] :entity/position :keys [entity/body]} c]
    (draw-text c
               {:text text
                :x x
                :y (+ y (:half-height body) (pixels->world-units c hpbar-height-px))
                :scale 2
                :up? true})))

(defmethod cdq.context/transact! :tx/add-text-effect [[_ entity text] ctx]
  [[:tx/assoc
    entity
    :entity/string-effect
    (if-let [string-effect (:entity/string-effect @entity)]
      (-> string-effect
          (update :text str "\n" text)
          (update :counter #(reset ctx %)))
      {:text text
       :counter (->counter ctx 0.4)})]])
