(ns cdq.entity.state.npc-sleeping
  (:require [gdl.context :refer [draw-text]]
            [gdl.graphics.color :as color]
            [cdq.entity.state :as state]
            [cdq.context :refer [world-grid ->counter]]
            [cdq.entity :as entity]
            [cdq.world.cell :as cell]))

; TODO pass to creature data, also @ shout
(def ^:private aggro-range 6)

(defrecord NpcSleeping [entity]
  state/State
  (enter [_ _ctx])

  (exit [_ ctx]
    ; TODO make state = alerted, and shout at the end of that !
    ; then nice alert '!' and different entities different alert time
    [(entity/add-text-effect @entity ctx "!")
     #:entity {:position (:entity/position @entity)
               :faction  (:entity/faction  @entity)
               :shout (->counter ctx 0.2)}])

  (tick [_ context]
    (let [cell (get (world-grid context)
                    (utils.core/->tile (:entity/position @entity)))]
      (when-let [distance (cell/nearest-entity-distance @cell
                                                        (entity/enemy-faction @entity))]
        (when (<= distance (* aggro-range 10)) ; TODO do @ cell/nearest-enemy-distance
          [[:tx/event entity :alert]]))))

  (render-below [_ c entity*])
  (render-above [_ c {[x y] :entity/position :keys [entity/body]}]
    (draw-text c
               {:text "zzz"
                :x x
                :y (+ y (:half-height body))
                :up? true}))
  (render-info [_ c entity*]))
