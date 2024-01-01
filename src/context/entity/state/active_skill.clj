(ns context.entity.state.active-skill
  (:require [gdl.context :refer [draw-filled-circle draw-sector draw-image play-sound!]]
            [context.entity.state :as state]
            [cdq.context :refer [valid-params? do-effect! effect-render-info send-event!
                                  stopped? finished-ratio ->counter set-skill-to-cooldown! pay-skill-mana-cost! set-cursor!]]
            [cdq.entity :as entity]))

(defn- draw-skill-icon [c icon entity* [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ width 2)
        y (+ y (:half-height (:body entity*)))
        center [x (+ y radius)]]
    (draw-filled-circle c center radius [1 1 1 0.125])
    (draw-sector c center radius
                 90 ; start-angle
                 (* action-counter-ratio 360) ; degree
                 [1 1 1 0.5])
    (draw-image c icon [(- x radius) y])))

(defrecord ActiveSkill [entity skill effect-context counter]
  state/PlayerState
  (player-enter [_ ctx]
    (set-cursor! ctx :cursors/sandclock))

  (pause-game? [_] false)
  (manual-tick! [_ context])

  state/State
  (enter [_ context]
    ; TODO all only called here => start-skill-bla
    ; make all this context context.entity.skill extension ?
    (play-sound! context (str "sounds/" (if (:spell? skill) "shoot.wav" "slash.wav")))
    (set-skill-to-cooldown! context entity skill)
    ; should assert enough mana
    ; but should also assert usable-state = :usable
    ; but do not want to call again valid-params? (expensive)
    ; i know i do it before only @ player & creature idle so ok
    (pay-skill-mana-cost! context entity skill))

  (exit [_ _ctx])

  (tick! [_ context]
    (let [effect (:effect skill)
          effect-context (merge context effect-context)]
      (cond
       (not (valid-params? effect-context effect))
       (send-event! context entity :action-done)

       (stopped? context counter)
       (do
        (do-effect! effect-context effect)
        (send-event! context entity :action-done)))))

  (render-below [_ c entity*])
  (render-above [_ c entity*])
  (render-info [_ c {:keys [position] :as entity*}]
    (let [{:keys [property/image effect]} skill]
      (draw-skill-icon c image entity* position (finished-ratio c counter))
      (effect-render-info (merge c effect-context) effect))))

(defn- apply-action-speed-modifier [entity* skill action-time]
  (let [{:keys [cast-speed attack-speed]} (:modifiers entity*)
        modified-action-time (/ action-time
                                (or (if (:spell? skill)
                                      cast-speed
                                      attack-speed)
                                    1))]
    (max 0 modified-action-time)))

(defn ->CreateWithCounter [context entity [skill effect-context]]
  ; assert keys effect-context only with 'effect/'
  ; so we don't use an outdated 'context' in the State update
  ; when we call State protocol functions we call it with the current context
  (assert (every? #(= "effect" (namespace %)) (keys effect-context)))
  (->ActiveSkill entity
                 skill
                 effect-context
                 (->> skill
                      :action-time
                      (apply-action-speed-modifier @entity skill)
                      (->counter context))))
