(ns game.components.state.active-skill
  (:require [gdl.graphics.draw :as draw]
            [gdl.graphics.color :as color]
            [data.counter :as counter]
            [data.val-max :refer [apply-val]]
            [game.effect :as effect]
            [game.protocols :as gm]
            [game.components.state :as state]
            [game.components.skills :as skills]))

(defn- draw-skill-icon [drawer icon entity* [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ width 2)
        y (+ y (:half-height (:body entity*)))
        center [x (+ y radius)]]
    (draw/filled-circle drawer center radius (color/rgb 1 1 1 0.125))
    (draw/sector drawer center radius
                 0 ; start-angle
                 (* action-counter-ratio 360) ; degree
                 (color/rgb 1 1 1 0.5))
    (draw/image drawer icon [(- x radius) y])))

(defrecord State [entity skill effect-params counter]
  state/PlayerState
  (pause-game? [_] false)
  (manual-tick! [_ context delta])

  state/State
  (enter [_ context]
    (gm/play-sound! context (str "sounds/" (if (:spell? skill) "shoot.wav" "slash.wav")))
    (swap! entity update :skills skills/set-skill-to-cooldown skill)
    ; should assert enough mana
    ; but should also assert usable-state = :usable
    ; but do not want to call again valid-params? (expensive)
    ; i know i do it before only @ player & creature idle so ok
    (swap! entity (fn [entity*]
                    (if (:cost skill)
                      (update entity* :mana apply-val #(- % (:cost skill)))
                      entity*))))

  (exit [_ _ctx])

  (tick [this delta]
    (update this :counter counter/tick delta))

  (tick! [_ context delta]
    (let [effect (:effect skill)]
      (cond
       (not (effect/valid-params? effect effect-params))
       (state/send-event! context entity :action-done)

       (counter/stopped? counter)
       (do
        (effect/do! effect effect-params context)
        (state/send-event! context entity :action-done)))))

  (render-below [_ drawer context entity*])
  (render-above [_ drawer context entity*])
  (render-info [_ drawer context {:keys [position] :as entity*}]
    (let [{:keys [image effect]} skill]
      (draw-skill-icon drawer image entity* position (counter/ratio counter))
      (effect/render-info drawer effect effect-params))))

(defn- apply-action-speed-modifier [entity* skill action-time]
  (let [{:keys [cast-speed attack-speed]} (:modifiers entity*)
        modified-action-time (/ action-time
                                (or (if (:spell? skill)
                                      cast-speed
                                      attack-speed)
                                    1))]
    (max 0 (int modified-action-time))))

(defn ->CreateWithCounter [entity skill effect-params]
  (->State entity
           skill
           effect-params
           (counter/create (apply-action-speed-modifier @entity
                                                        skill
                                                        (:action-time skill)))))
