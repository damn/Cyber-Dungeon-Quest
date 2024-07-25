(ns cdq.entity.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics :as g]
            [cdq.api.context :refer [->counter stopped? reset]]
            [cdq.context.ui.config :refer [hpbar-height-px]]
            [cdq.api.entity :as entity]))

(defcomponent :entity/string-effect {}
  {:keys [text counter] :as this}
  (entity/tick [[k _] {:keys [entity/id]} context]
    (when (stopped? context counter)
      [[:tx/dissoc id k]]))

  (entity/render-above [_ {[x y] :entity/position :keys [entity/body]} g _ctx]
    (g/draw-text g
                 {:text text
                  :x x
                  :y (+ y (:half-height body) (g/pixels->world-units g hpbar-height-px))
                  :scale 2
                  :up? true})))

(defmethod cdq.api.context/transact! :tx/add-text-effect [[_ entity text] ctx]
  [[:tx/assoc
    entity
    :entity/string-effect
    (if-let [string-effect (:entity/string-effect @entity)]
      (-> string-effect
          (update :text str "\n" text)
          (update :counter #(reset ctx %)))
      {:text text
       :counter (->counter ctx 0.4)})]])
