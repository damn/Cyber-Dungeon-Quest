(ns game.effects.damage
  (:require [gdl.audio :as audio]
            [utils.core :refer :all]
            [game.media :as media]
            [game.components.modifiers :as modifiers]
            [game.effect :as effect]
            [game.utils.random :as random]
            [game.components.string-effect :refer [show-string-effect]]
            [game.entities.animation :as animation-entity]
            [game.components.hp :refer (dead?)]))

(defn- effect-modifiers [{:keys [source target]} effect-type]
  {:source-modifiers (-> @source :effect-modifiers effect-type :source)
   :target-modifiers (when target
                       (-> @target :effect-modifiers effect-type :target))})

(defn- blocks? [shield-or-armor {:keys [source target value] :as params}]
  (let [[damage-type damage] value
        {:keys [source-modifiers
                target-modifiers]} (effect-modifiers params shield-or-armor)
        source-block-ignore (damage-type source-modifiers)
        target-block        (damage-type target-modifiers)
        block-rate (max (- (or target-block 0)
                           (or source-block-ignore 0))
                        0)]
    (< (rand) block-rate)))

(defn- apply-damage-modifiers [{:keys [source target value] :as params}]
  (let [[damage-type damage] value
        {:keys [source-modifiers
                target-modifiers]} (effect-modifiers params :damage)
        source-modifiers (damage-type source-modifiers)
        target-modifiers (damage-type target-modifiers)
        damage (if source-modifiers
                 (apply-val-max-modifiers damage source-modifiers)
                 damage)
        damage (if target-modifiers
                 (apply-val-max-modifiers damage target-modifiers)
                 damage)]
    [damage-type damage]))

(comment
 ; TODO is outdated, redo.
 (apply-modifiers
  {:source-modifiers {:physical  {[:max :mult] 2
                                  [:val :mult] 1.5
                                  [:val :inc] 1
                                  [:max :inc] 0
                                  :armor-ignore 0
                                  :shield-ignore 0}}
   :target-modifiers {:physical  {[:max :mult] 1
                                  [:val :mult] 1
                                  [:val :inc] -5
                                  [:max :inc] 0
                                  :armor-block 0.3
                                  :shield-block 0.5}}
   :value [:physical [3 10]]})
 ; =>
 {:damage [6 20]} ; or shield or armor
 )

(defn- shield-blocked-effect [entity]
  (show-string-effect entity "SHIELD"))

(defn- armor-blocked-effect [entity]
  ; (audio/play "bfxr_armorhit.wav")
  (show-string-effect entity "ARMOR"))

(defn- damage-type->hit-effect! [damage-type position]
  (let [[sound fx-idx] (case damage-type
                         :physical ["bfxr_normalhit.wav" [3 0]]
                         :magic    ["bfxr_curse.wav"     [6 1]])]
    (audio/play sound)
    (animation-entity/create!
     :position position
     :animation (media/fx-impact-animation fx-idx))))

(defn- deal-damage! [{:keys [target] [damage-type] :value :as params}]
  (when-not (dead? @target)
    (cond
     (blocks? :shield params) (shield-blocked-effect target)
     (blocks? :armor  params) (armor-blocked-effect  target)
     :else
     (let [[damage-type damage] (apply-damage-modifiers params)
           damage (random/rand-int-between damage)]
       (damage-type->hit-effect! damage-type
                                 (:position @target))
       (effect/do-effect! {:target target}
                           [:hp [[:val :inc] (- damage)]])))))

#_(defn- get-dps [[mi mx] seconds]
  (round-n-decimals (/ (+ mi mx) 2 seconds) 2))

#_(defn- get-sword-infostr [{:keys [base-dmg] :as skill}]
  (let [seconds 0.3]
    (str
      (variance-val-str base-dmg) " Damage\n"
      "Normal " " Attack-Speed\n"
      (str "DPS: " (get-dps base-dmg seconds)))))

(defn- damage-text [[damage-type damage-amount]]
  (str (damage-amount 0) "-" (damage-amount 1) " " (name damage-type) " damage"))

(defn- damage-infotext
  [{:keys [value] :as params}] ; value = [:physical [5 6]] for example
  (let [modified (apply-damage-modifiers params)]
    (if (= value modified)
      (damage-text value)
      (str
       (damage-text value)
       "\n"
       "Modified: " (damage-text (apply-damage-modifiers params))))))

; example:
; [:damage [:physical [5 6]]]
(comment
 ; malli:
 [:tuple [:enum :damage]
  [:tuple [:enum :physical :magic]
   [:tuple int? int?]]]
 ; TODO => val-max-data as schema!
 ; two >=0 integers and val<=max.
 )
(effect/defeffect :damage
  {:text damage-infotext
   :valid-params? (fn [{:keys [source target]}]
                    (and source
                         target
                         (:hp @target)))
   :do! deal-damage!})

(defn- check-shield_armor-value [[source-or-target
                                  damage-type
                                  value-delta]]
  (and (#{:source :target} source-or-target)
       (#{:physical :magic} damage-type)))

; TODO == effect-modifier-modifier O.O
(defn- shield_armor-modifier [shield_armor]
  {:text (fn [value _]
           (assert (check-shield_armor-value value)
                   (str "Wrong value for modifier: " value))
           (str value))
   :keys [:effect-modifiers shield_armor]
   :apply (fn [component value]
            (assert (check-shield_armor-value value)
                    (str "Wrong value for shield/armor modifier: " value))
            (update-in component (drop-last value) #(+ (or % 0) (last value))))
   :reverse (fn [component value]
              (update-in component (drop-last value) - (last value)))})

; Example: [:shield [:target :physical 0.3]]
(modifiers/defmodifier :shield
  (shield_armor-modifier :shield))

(modifiers/defmodifier :armor
  (shield_armor-modifier :armor))

(defn- check-damage-modifier-value [[source-or-target
                                     damage-type
                                     application-type
                                     value-delta]]
  (and (#{:source :target} source-or-target)
       (#{:physical :magic} damage-type)
       (let [[val-or-max inc-or-mult] application-type]
         (and (#{:val :max} val-or-max)
              (#{:inc :mult} inc-or-mult)))))

(defn- default-value [application-type]
  (let [[val-or-max inc-or-mult] application-type]
    (case inc-or-mult
      :inc 0
      :mult 1)))

(defn- damage-modifier-text [[source-or-target
                              damage-type
                              application-type
                              value-delta]]

  (str (name damage-type)
       (when (:target source-or-target)
         " received ")

       )
  )

; example: [:damage [:source :physical [:max :mult] 3]]
; TODO => effect-modifier-modifier
(modifiers/defmodifier :damage
  {:text (fn [value _]
           (assert (check-damage-modifier-value value)
                   (str "Wrong value for damage modifier: " value))
           (damage-modifier-text value))
   :keys [:effect-modifiers :damage]
   :apply (fn [component value]
            (assert (check-damage-modifier-value value)
                    (str "Wrong value for damage modifier: " value))
            (update-in component (drop-last value) #(+ (or % (default-value (get value 2)))
                                                       (last value))))
   :reverse (fn [component value]
              (assert (check-damage-modifier-value value)
                      (str "Wrong value for damage modifier: " value))
              (update-in component (drop-last value) - (last value)))})
