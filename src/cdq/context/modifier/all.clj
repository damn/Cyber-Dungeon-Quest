(ns cdq.context.modifier.all
  (:require [clojure.string :as str]
            [clojure.math :as math]
            [data.val-max :refer [apply-max]]
            [cdq.context.modifier :as modifier]))

; TODO add movement speed +/- modifier.

(defn- check-plus-symbol [n]
  (case (math/signum n)
    (0.0 1.0) "+"
    -1.0 ""))

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

(defn- actions-speed-percent [v]
  (let [v (dec v)]
    (str (check-plus-symbol v) (int (* 100 v)))))

(modifier/defmodifier :modifier/cast-speed
  {:text    #(str (actions-speed-percent %) "% Casting-Speed")
   :keys    [:entity/stats :stats/cast-speed]
   :apply   #(+ (or %1 1) %2)
   :reverse -})

(modifier/defmodifier :modifier/attack-speed
  {:text    #(str (actions-speed-percent %) "% Attack-Speed")
   :keys    [:entity/stats :stats/attack-speed]
   :apply   #(+ (or %1 1) %2)
   :reverse -})

(defn- check-block-modifier-value [[source-or-target
                                    damage-type
                                    value-delta]]
  (and (#{:block/ignore :block/rate} source-or-target)
       (#{:physical :magic} damage-type)))

(defn- dmg-type-text [damage-type]
  (str (str/capitalize (name damage-type)) " damage"))

(defn- block-modifier-text [block-type [source-or-target damage-type value-delta]]
  (str/join " "
            [(dmg-type-text damage-type)
             (case block-type
               :stats/shield "shield"
               :stats/armor  "armor")
             (case source-or-target
               :block/ignore "ignore"
               :block/rate "block")
             ; TODO signum ! negativ possible?
             (str "+" (int (* value-delta 100)) "%")]))

(defn- block-modifier [block-type]
  {:text (fn [value]
           (assert (check-block-modifier-value value)
                   (str "Wrong value for modifier: " value))
           (block-modifier-text block-type value))
   :keys [:entity/stats block-type]
   :apply (fn [component value]
            (assert (check-block-modifier-value value)
                    (str "Wrong value for shield/armor modifier: " value))
            (update-in component (drop-last value) #(+ (or % 0) (last value))))
   :reverse (fn [component value]
              (update-in component (drop-last value) - (last value)))})

(modifier/defmodifier :modifier/shield (block-modifier :stats/shield))
(modifier/defmodifier :modifier/armor  (block-modifier :stats/armor))

(defn- check-damage-modifier-value [[source-or-target
                                     damage-type
                                     application-type
                                     value-delta]]
  (and (#{:damage/deal :damage/receive} source-or-target)
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
               :damage/deal "dealt"
               :damage/receive "received")
             ; TODO not handling negative values yet (do I need that ?)
             (case inc-or-mult
               :inc "+"
               :mult "+")
             (case inc-or-mult
               :inc value-delta
               :mult (str (int (* value-delta 100)) "%"))]))

(modifier/defmodifier :modifier/damage
  {:text (fn [value]
           (assert (check-damage-modifier-value value)
                   (str "Wrong value for damage modifier: " value))
           (damage-modifier-text value))
   :keys [:entity/stats :stats/damage]
   :apply (fn [component value]
            (assert (check-damage-modifier-value value)
                    (str "Wrong value for damage modifier: " value))
            (update-in component (drop-last value) #(+ (or % (default-value (get value 2)))
                                                       (last value))))
   :reverse (fn [component value]
              (assert (check-damage-modifier-value value)
                      (str "Wrong value for damage modifier: " value))
              (update-in component (drop-last value) - (last value)))})
