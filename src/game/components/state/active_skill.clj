(ns game.components.state.active-skill
  (:require [gdl.graphics.color :as color]
            [gdl.context :refer [draw-filled-circle draw-sector draw-image play-sound!]]
            [data.counter :as counter]
            [data.val-max :refer [apply-val]]
            [game.context :refer [valid-params? do-effect! send-event!]]
            [game.entity :as entity]
            [game.components.skills :as skills]))

(defn- draw-skill-icon [c icon entity* [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ width 2)
        y (+ y (:half-height (:body entity*)))
        center [x (+ y radius)]]
    (draw-filled-circle c center radius (color/rgb 1 1 1 0.125))
    (draw-sector c center radius
                 0 ; start-angle
                 (* action-counter-ratio 360) ; degree
                 (color/rgb 1 1 1 0.5))
    (draw-image c icon [(- x radius) y])))

(defrecord State [entity skill effect-params counter]
  entity/PlayerState
  (pause-game? [_] false)
  (manual-tick! [_ context delta])

  entity/State
  (enter [_ context]
    (play-sound! context (str "sounds/" (if (:spell? skill) "shoot.wav" "slash.wav")))
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
       ; TODO Check namespaced params
       (not (valid-params? (merge context effect-params) effect))
       (send-event! context entity :action-done)

       (counter/stopped? counter)
       (do
        (do-effect! (merge context effect-params) [effect])
        (send-event! context entity :action-done)))))

  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info [_ c {:keys [position] :as entity*}]
    (let [{:keys [image effect]} skill]
      (draw-skill-icon c image entity* position (counter/ratio counter))
      ; TODO ? render fns ?!
      (effect/render-info c effect effect-params))))

(defn- apply-action-speed-modifier [entity* skill action-time]
  (let [{:keys [cast-speed attack-speed]} (:modifiers entity*)
        modified-action-time (/ action-time
                                (or (if (:spell? skill)
                                      cast-speed
                                      attack-speed)
                                    1))]
    (max 0 (int modified-action-time))))

(defn ->CreateWithCounter [entity [skill effect-params]]
  (->State entity
           skill
           effect-params
           (counter/create (apply-action-speed-modifier @entity
                                                        skill
                                                        (:action-time skill)))))
