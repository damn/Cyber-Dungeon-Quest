(ns game.components.state.npc-sleeping
  (:require [gdl.graphics.draw :as draw]
            [data.counter :as counter]
            [game.protocols :as gm]
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

  (render-below [_ drawer context entity*])
  (render-above [_ drawer _ctx {[x y] :position :keys [body]}]
    (draw/text drawer
               {:text "zzz"
                :x x
                :y (+ y (:half-height body))
                :up? true}))
  (render-info [_ drawer _ctx {:keys [position mouseover?]}]
    (when mouseover? ; TODO is not exact, using tile distance, (remove ?)
      (draw/circle drawer position aggro-range Color/YELLOW))))
