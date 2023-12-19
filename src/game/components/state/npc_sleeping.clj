(ns game.components.state.npc-sleeping
  (:require [gdl.context :refer [draw-text draw-circle]]
            [data.counter :as counter]
            [game.context :as gm]
            [game.components.state :as state]
            [game.components.string-effect :as string-effect]
            [game.components.faction :as faction])
  (:import com.badlogic.gdx.graphics.Color))

(def ^:private aggro-range 6)

(defrecord State [entity]
  state/State
  (enter [_ _ctx])

  (exit [_ context]
    (swap! entity string-effect/add "!")
    (gm/create-entity! context
                       {:position (:position @entity)
                        :faction (:faction  @entity)
                        :shout (counter/create 200)}) )

  (tick [this delta] this)
  (tick! [_ {:keys [context/world-map] :as context} delta]
    (let [cell-grid (:cell-grid world-map)
          cell* @(get cell-grid (mapv int (:position @entity)))
          faction (faction/enemy (:faction @entity))]
      (when-let [distance (-> cell* faction :distance)]
        (when (<= distance (* aggro-range 10))
          (state/send-event! context entity :alert)))))

  (render-below [_ c entity*])
  (render-above [_ c {[x y] :position :keys [body]}]
    (draw-text c
               {:text "zzz"
                :x x
                :y (+ y (:half-height body))
                :up? true}))
  (render-info [_ c {:keys [position mouseover?]}]
    (when mouseover? ; TODO is not exact, using tile distance, (remove ?)
      (draw-circle c position aggro-range Color/YELLOW))))
