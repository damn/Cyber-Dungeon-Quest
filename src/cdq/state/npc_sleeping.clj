(ns cdq.state.npc-sleeping
  (:require [gdl.context :refer [draw-text]]
            [gdl.graphics.color :as color]
            [cdq.context :refer [world-grid ->counter]]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.world.cell :as cell]))

; TODO pass to creature data, also @ shout
(def ^:private aggro-range 6)

(defrecord NpcSleeping []
  state/State
  (enter [_ entity* _ctx])

  (exit [_ entity* ctx]
    ; TODO make state = alerted, and shout at the end of that !
    ; then nice alert '!' and different entities different alert time
    [(entity/add-text-effect entity* ctx "!")
     #:entity {:position (:entity/position entity*)
               :faction  (:entity/faction  entity*)
               :shout (->counter ctx 0.2)}])

  (tick [_ entity* context]
    (let [cell (get (world-grid context)
                    (utils.core/->tile (:entity/position entity*)))]
      (when-let [distance (cell/nearest-entity-distance @cell (entity/enemy-faction entity*))]
        (when (<= distance (* aggro-range 10)) ; TODO do @ cell/nearest-enemy-distance
          [[:tx/event (:entity/id entity*) :alert]]))))

  (render-below [_ entity* c])
  (render-above [_ {[x y] :entity/position :keys [entity/body]} c]
    (draw-text c
               {:text "zzz"
                :x x
                :y (+ y (:half-height body))
                :up? true}))
  (render-info [_ entity* c]))
