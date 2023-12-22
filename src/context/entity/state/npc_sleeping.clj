(ns context.entity.state.npc-sleeping
  (:require [gdl.context :refer [draw-text draw-circle]]
            [gdl.graphics.color :as color]
            [game.context :refer [world-grid create-entity! send-event! ->counter]]
            [context.entity.state :as state]
            [game.world.cell :as cell]))

; TODO pass to creature data, also @ shout
(def ^:private aggro-range 6)

(defrecord NpcSleeping [entity]
  state/State
  (enter [_ _ctx])

  (exit [_ context]
    ; TODO make state = alerted, and shout at the end of that !
    ; then nice alert '!' and different entities different alert time
    (add-text-effect! context entity "!")
    (create-entity! context
                    {:position (:position @entity)
                     :faction (:faction  @entity)
                     :shout (->counter context 200)}))

  (tick! [_ context delta]
    (let [cell (get (world-grid context)
                    (utils.core/->tile (:position @entity)))]
      (when-let [distance (cell/nearest-enemy-distance @cell (:faction @entity))]
        (when (<= distance (* aggro-range 10)) ; TODO do @ cell/nearest-enemy-distance
          (send-event! context entity :alert)))))

  (render-below [_ c entity*])
  (render-above [_ c {[x y] :position :keys [body]}]
    (draw-text c
               {:text "zzz"
                :x x
                :y (+ y (:half-height body))
                :up? true}))
  (render-info [_ c {:keys [position mouseover?]}]
    (when mouseover? ; TODO is not exact, using tile distance, (remove ?)
      (draw-circle c position aggro-range color/yellow))))
