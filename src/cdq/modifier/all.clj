(ns cdq.modifier.all
  (:require [clojure.string :as str]
            [clojure.math :as math]
            [x.x :refer [defcomponent]]
            [data.val-max :refer [apply-max]]
            [cdq.modifier :as modifier]))

; TODO add movement speed +/- modifier.

(defn- check-plus-symbol [n]
  (case (math/signum n)
    (0.0 1.0) "+"
    -1.0 ""))

(defn- plus-max-modifier-text [modified-value-name v]
  (str (check-plus-symbol v) v " " modified-value-name))

(defn- apply-max-plus  [vmx v] (apply-max vmx #(+ % v)))
(defn- apply-max-minus [vmx v] (apply-max vmx #(- % v)))

(defcomponent :modifier/max-hp amount
  (modifier/text [_] (plus-max-modifier-text "HP" amount))
  (modifier/keys [_] [:entity/hp])
  (modifier/apply   [_ hp] (apply-max-plus  hp amount))
  (modifier/reverse [_ hp] (apply-max-minus hp amount)))

(defcomponent :modifier/max-mana amount
  (modifier/text [_] (plus-max-modifier-text "Mana" amount))
  (modifier/keys [_] [:entity/mana])
  (modifier/apply   [_ mana] (apply-max-plus  mana amount))
  (modifier/reverse [_ mana] (apply-max-minus mana amount)))

(defn- actions-speed-percent [v]
  (str (check-plus-symbol v) (int (* 100 v))))

(defcomponent :modifier/cast-speed amount
  (modifier/text [_] (str (actions-speed-percent amount) "% Casting-Speed"))
  (modifier/keys [_] [:entity/stats :stats/cast-speed])
  (modifier/apply   [_ value] (+ (or value 1) amount))
  (modifier/reverse [_ value] (- value amount)))

(defcomponent :modifier/attack-speed amount
  (modifier/text [_] (str (actions-speed-percent amount) "% Attack-Speed"))
  (modifier/keys [_] [:entity/stats :stats/attack-speed])
  (modifier/apply   [_ value] (+ (or value 1) amount))
  (modifier/reverse [_ value] (- value amount)))

(defn- dmg-type-text [damage-type]
  (str (str/capitalize (name damage-type)) " damage"))

(defn- armor-modifier-text [modifier-attribute [damage-type value-delta]]
  (str/join " "
            [(name modifier-attribute)
             (dmg-type-text damage-type)
             ; TODO signum ! negativ possible?
             (str "+" (int (* value-delta 100)) "%")]))

(defn- apply-block-stat [stat [damage-type value-delta]]
  (update stat damage-type #(+ (or % 0) value-delta)))

(defn- reverse-block-stat [stat [damage-type value-delta]]
  (update stat damage-type - value-delta))

; TODO
; stat: existing value of stat accessed through modifier/keys.
; call maybe 'value' and 'delta'

(defcomponent :modifier/armor-save value
  (modifier/text [[k _]] (armor-modifier-text k value))
  (modifier/keys [_] [:entity/stats :stats/armor-save])
  (modifier/apply   [_ stat] (apply-block-stat   stat value))
  (modifier/reverse [_ stat] (reverse-block-stat stat value)))

(defcomponent :modifier/armor-pierce value
  (modifier/text [_] (armor-modifier-text value))
  (modifier/keys [_] [:entity/stats :stats/armor-pierce])
  (modifier/apply   [_ stat] (apply-block-stat   stat value))
  (modifier/reverse [_ stat] (reverse-block-stat stat value)))

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

(defcomponent :modifier/damage value
  (modifier/text [_]
    (assert (check-damage-modifier-value value)
            (str "Wrong value for damage modifier: " value))
    (damage-modifier-text value))
  (modifier/keys [_] [:entity/stats :stats/damage])
  (modifier/apply [_ stat]
    (assert (check-damage-modifier-value value)
            (str "Wrong value for damage modifier: " value))
    (update-in stat (drop-last value) #(+ (or % (default-value (get value 2)))
                                          (last value))))
  (modifier/reverse [_ stat]
    (assert (check-damage-modifier-value value)
            (str "Wrong value for damage modifier: " value))
    (update-in stat (drop-last value) - (last value))))
