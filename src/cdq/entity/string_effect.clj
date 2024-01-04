(ns cdq.entity.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-text pixels->world-units]]
            [cdq.context :refer [->counter stopped? reset]]
            [cdq.context.ui.config :refer [hpbar-height-px]]
            [cdq.entity :as entity]))

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

(extend-type cdq.entity.Entity
  cdq.entity/TextEffect
  (add-text-effect [entity* ctx text]
    (if (:entity/string-effect entity*)
      (update entity* :entity/string-effect #(-> %
                                                 (update :text str "\n" text)
                                                 (update :counter (fn [cnt]
                                                                    (reset ctx cnt)))))
      (assoc entity* :entity/string-effect {:text text
                                            :counter (->counter ctx 0.4)}))))
