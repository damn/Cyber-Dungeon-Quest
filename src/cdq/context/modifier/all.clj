(ns cdq.context.modifier.all
  (:require [clojure.string :as str]
            [clojure.math :as math]
            [utils.core :refer [readable-number]]
            [data.val-max :refer [apply-max]]
            [cdq.context.modifier :as modifier]))

; TODO add movement speed +/- modifier.

(defn- check-plus-symbol [n]
  (case (math/signum n) 1.0 "+" -1.0 ""))

(defn- plus-max-modifier-text [modified-value-name v]
  (str (check-plus-symbol v) v " " modified-value-name))

(defn- apply-max-plus  [vmx v] (apply-max vmx #(+ % v)))
(defn- apply-max-minus [vmx v] (apply-max vmx #(- % v)))

(modifier/defmodifier :modifier/max-hp
  {:text    (partial plus-max-modifier-text "HP")
   :keys    [:entity/hp]
   :apply   apply-max-plus
   :reverse apply-max-minus})

(modifier/defmodifier :modifier/max-mana
  {:text    (partial plus-max-modifier-text "Mana")
   :keys    [:entity/mana]
   :apply   apply-max-plus
   :reverse apply-max-minus})

; TODO (/ action-time cast-speed)
; new calculations

(modifier/defmodifier :modifier/cast-speed
  {:text    (fn [v] (str "+" v "% Casting-Speed"))
   :keys    [:entity/modifiers :cast-speed]
   :apply   #(+ (or %1 1) (/ %2 100))
   :reverse #(- %1 (/ %2 100))})

(modifier/defmodifier :modifier/attack-speed
  {:text    (fn [v] (str "+" v "% Attack-Speed"))
   :keys    [:entity/modifiers :attack-speed]
   :apply   #(+ (or %1 1) (/ %2 100))
   :reverse #(- %1 (/ %2 100))})

(defn- check-damage-block-modifier-value [[source-or-target
                                           damage-type
                                           value-delta]]
  (and (#{:effect/source :effect/target} source-or-target) ; TODO use other names (block-absorb,block-ignore?)
       (#{:physical :magic} damage-type)))

(defn- dmg-type-text [damage-type]
  (str (str/capitalize (name damage-type)) " damage"))

(defn- damage-block-modifier-text [block-type
                                   [source-or-target
                                    damage-type
                                    value-delta]]
  (str/join " "
            [(dmg-type-text damage-type)
             (case block-type
               :shield "shield"
               :armor  "armor")
             (case source-or-target
               :effect/source "ignore"
               :effect/target "block")
             ; TODO signum ! negativ possible?
             (str "+" (int (* value-delta 100)) "%")]))

(defn- damage-block-modifier [block-type]
  {:text (fn [value]
           (assert (check-damage-block-modifier-value value)
                   (str "Wrong value for modifier: " value))
           (damage-block-modifier-text block-type value))
   :keys [:entity/modifiers block-type]
   :apply (fn [component value]
            (assert (check-damage-block-modifier-value value)
                    (str "Wrong value for shield/armor modifier: " value))
            (update-in component (drop-last value) #(+ (or % 0) (last value))))
   :reverse (fn [component value]
              (update-in component (drop-last value) - (last value)))})

; Example: [:modifier/shield [:effect/target :physical 0.3]]
(modifier/defmodifier :modifier/shield
  (damage-block-modifier :shield))

(modifier/defmodifier :modifier/armor
  (damage-block-modifier :armor))

(defn- check-damage-modifier-value [[source-or-target
                                     damage-type
                                     application-type
                                     value-delta]]
  (and (#{:effect/source :effect/target} source-or-target)
       (#{:physical :magic} damage-type)
       (let [[val-or-max inc-or-mult] application-type]
         (and (#{:val :max} val-or-max)
              (#{:inc :mult} inc-or-mult)))))

(defn- default-value [application-type] ; TODO here too !
  (let [[val-or-max inc-or-mult] application-type]
    (case inc-or-mult
      :inc 0
      :mult 1)))

(defn- damage-modifier-text [[source-or-target
                              damage-type
                              [val-or-max inc-or-mult]
                              value-delta]]
  (str/join " "
            [(case val-or-max
                      :val "Minimum"
                      :max "Maximum")
             (dmg-type-text damage-type)
             (case source-or-target
               :effect/source "dealt"
               :effect/target "received")
             ; TODO not handling negative values yet (do I need that ?)
             (case inc-or-mult
               :inc "+"
               :mult "+")
             (case inc-or-mult
               :inc value-delta
               :mult (str (int (* value-delta 100)) "%"))]))

; example: [:damage [:effect/source :physical [:max :mult] 3]]
(modifier/defmodifier :modifier/damage
  {:text (fn [value]
           (assert (check-damage-modifier-value value)
                   (str "Wrong value for damage modifier: " value))
           (damage-modifier-text value))
   :keys [:entity/modifiers :tx/damage]
   :apply (fn [component value]
            (assert (check-damage-modifier-value value)
                    (str "Wrong value for damage modifier: " value))
            (update-in component (drop-last value) #(+ (or % (default-value (get value 2)))
                                                       (last value))))
   :reverse (fn [component value]
              (assert (check-damage-modifier-value value)
                      (str "Wrong value for damage modifier: " value))
              (update-in component (drop-last value) - (last value)))})
