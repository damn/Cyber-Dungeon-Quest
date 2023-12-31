(ns cdq.state.active-skill
  (:require [gdl.context :refer [draw-filled-circle draw-sector draw-image]]
            [data.val-max :refer [apply-val]]
            [cdq.context :refer [valid-params? effect-render-info stopped? finished-ratio ->counter]]
            [cdq.entity :as entity]
            [cdq.state :as state]))

(defn- draw-skill-icon [c icon entity* [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ width 2)
        y (+ y (:half-height (:entity/body entity*)))
        center [x (+ y radius)]]
    (draw-filled-circle c center radius [1 1 1 0.125])
    (draw-sector c center radius
                 90 ; start-angle
                 (* action-counter-ratio 360) ; degree
                 [1 1 1 0.5])
    (draw-image c icon [(- x radius) y])))

(defn- set-skill-to-cooldown [entity* {:keys [property/id skill/cooldown] :as skill} ctx]
  (when cooldown
    [:tx/assoc-in (:entity/id entity*) [:entity/skills id :skill/cooling-down?] (->counter ctx cooldown)]))

(defn- pay-skill-mana-cost [{:keys [entity/id entity/mana]} {:keys [skill/cost]}]
  (when cost
    [:tx/assoc id :entity/mana (apply-val mana #(- % cost))]))

(defrecord ActiveSkill [skill effect-context counter]
  state/PlayerState
  (player-enter [_] [[:tx/cursor :cursors/sandclock]])
  (pause-game? [_] false)
  (manual-tick [_ entity* context])
  (clicked-inventory-cell [_ entity* cell])
  (clicked-skillmenu-skill [_ entity* skill])

  state/State
  ; should assert enough mana
  ; but should also assert usable-state = :usable
  ; but do not want to call again valid-params? (expensive)
  ; i know i do it before only @ player & creature idle so ok
  ; also mana cost could be part of effect ? idk. (immediate effect, not delayed)
  (enter [_ entity* ctx]
    [[:tx/sound (str "sounds/" (if (:spell? skill) "shoot.wav" "slash.wav"))]
     (set-skill-to-cooldown entity* skill ctx)
     (pay-skill-mana-cost entity* skill)])

  (exit [_ entity* _ctx])

  (tick [_ {:keys [entity/id]} context]
    (let [effect (:skill/effect skill)
          effect-context (merge context effect-context)]
      (cond
       (not (valid-params? effect-context effect))
       [[:tx/event id :action-done]]

       (stopped? context counter)
       [[:tx/effect effect-context effect]
        [:tx/event id :action-done]])))

  (render-below [_ entity* _ctx])
  (render-above [_ entity* _ctx])
  (render-info [_ {:keys [entity/position] :as entity*} ctx]
    (let [{:keys [property/image skill/effect]} skill]
      (draw-skill-icon ctx image entity* position (finished-ratio ctx counter))
      (effect-render-info (merge ctx effect-context) effect))))

(defn- apply-action-speed-modifier [entity* skill action-time]
  (let [{:keys [cast-speed attack-speed]} (:entity/modifiers entity*)
        modified-action-time (/ action-time
                                (or (if (:spell? skill) cast-speed attack-speed)
                                    1))]
    (max 0 modified-action-time)))

(defn ->CreateWithCounter [context entity* [skill effect-context]]
  ; assert keys effect-context only with 'effect/'
  ; so we don't use an outdated 'context' in the State update
  ; when we call State protocol functions we call it with the current context
  (assert (every? #(= "effect" (namespace %)) (keys effect-context)))
  (->ActiveSkill skill
                 effect-context
                 (->> skill
                      :skill/action-time
                      (apply-action-speed-modifier entity* skill)
                      (->counter context))))
