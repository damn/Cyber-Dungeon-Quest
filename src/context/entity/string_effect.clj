(ns context.entity.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.context :refer [draw-text pixels->world-units]]
            [context.ui.config :refer [hpbar-height-px]]
            [context.entity :as entity]
            [game.context :refer [stopped? reset]]))

(defcomponent :string-effect {:keys [text counter] :as this}
  (entity/tick! [[k _] e context delta]
    (when (stopped? context counter)
      (swap! e dissoc k)))
  (entity/render-above [_ {[x y] :position :keys [body]} c]
    (draw-text (update c :unit-scale * 2) ; TODO FIXME HACK implement :scale arg
               {:text text
                :x x
                :y (+ y (:half-height body)
                      (pixels->world-units c hpbar-height-px))
                :up? true})))

(extend-type gdl.context.Context
  game.context/TextEffect
  (add-text-effect! [context entity text]
    (if (:string-effect @entity)
      (swap! entity update :string-effect #(-> %
                                               (update :text str "\n" text)
                                               (update :counter #(reset context %))))
      (swap! entity assoc :string-effect {:text text
                                          :counter (->counter context 400)}))))
