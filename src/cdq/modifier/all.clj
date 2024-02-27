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
  (let [v (dec v)]
    (str (check-plus-symbol v) (int (* 100 v)))))

; TODO cast speed 2.3 not 1.3
; attack speed 2.5 not 1.5
; amount = '1.3'

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

(defn- check-block-modifier-value [[source-or-target
                                    damage-type
                                    value-delta]]
  (and (#{:block/ignore :block/rate} source-or-target)
       (#{:physical :magic} damage-type)))

(defn- dmg-type-text [damage-type]
  (str (str/capitalize (name damage-type)) " damage"))

; TODO assert modifier value  & props / attrs, not here
; tests ?
; not vector but map ! sub-component edit nested map edit !

(defn- block-modifier-text [block-type [source-or-target damage-type value-delta]]
  ; TODO mk map
  #_(assert (check-block-modifier-value value)
          (str "Wrong value for modifier: " value))
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

(defn- apply-block-stat [stat value]
  (assert (check-block-modifier-value value)
          (str "Wrong value for shield/armor modifier: " value))
  (update-in stat (drop-last value) #(+ (or % 0) (last value))))

(defn- reverse-block-stat [stat value]
  (update-in stat (drop-last value) - (last value)))

(defcomponent :modifier/shield value
  (modifier/text [_] (block-modifier-text :stats/shield value))
  (modifier/keys [_] [:entity/stats :stats/shield])
  (modifier/apply   [_ stat] (apply-block-stat   stat value))
  (modifier/reverse [_ stat] (reverse-block-stat stat value)))

(defcomponent :modifier/armor value
  (modifier/text [_] (block-modifier-text :stats/armor value))
  (modifier/keys [_] [:entity/stats :stats/armor])
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
