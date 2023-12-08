(ns game.modifiers.all
  (:require [utils.core :refer [readable-number]]
            [data.val-max :refer [apply-max]]
            [game.modifier :as modifier]
            [game.skills.core :as skills]
            [game.components.skills :as skills-component]))

; TODO dissoc again if value == default value -> into modifier logic, e.g. modifiers blocks 0 , just dissoc then ?
; TODO common modifiers add/decrement or multiplier, etc.
; TODO fix max-hp like max-mana

(defn- add-block [blocks ctype]
  (update blocks ctype #(inc (or % 0))))

(defn- remove-block [blocks ctype]
  {:pre  [(> (get blocks ctype) 0)]}
  (update blocks ctype dec))

(modifier/defmodifier :modifiers/block
  {:text     #(str "Stops " (name %))
   :keys     [:modifiers :blocks]
   :apply    add-block
   :reverse  remove-block})

(defn- modify-update-speed [update-speeds [ctype value]]
  (update update-speeds ctype #(+ (or % 1) value)))

(modifier/defmodifier :modifiers/update-speed
  {:values   [[8 20] [25 35] [40 50]]
   :text     #(str (Math/signum (float (% 1))) (% 1) "% " (% 0))
   :keys     [:modifiers :update-speed]
   :apply    modify-update-speed
   :reverse  #(modify-update-speed %1 [(%2 0) (- (%2 1))])})

(modifier/defmodifier :modifiers/max-hp
  {:values  [[15 25] [35 45] [55 65]]
   :text    #(str "+" % " HP")
   :keys    [:hp]
   :apply   (partial apply-max +) ; TODO broken, do like max-mana, all texts also broken.
   :reverse (partial apply-max -)})

(modifier/defmodifier :modifiers/max-mana
  {:values  [[15 25] [35 45] [55 65]]
   :text    (fn [v entity] (str "+" v " Mana"))
   :keys    [:mana]
   :apply   (fn [mana v] (apply-max mana #(+ % v)))
   :reverse (fn [mana v] (apply-max mana #(- % v)))})

(modifier/defmodifier :modifiers/skill
  {:text skills/text
   :keys [:skills]
   :apply skills-component/assoc-skill
   :reverse dissoc})

(modifier/defmodifier :modifiers/cast-speed
  {:values  [[15 25] [35 45] [50 60]]
   :text    (fn [v entity] (str "+" v "% Casting-Speed"))
   :keys    [:modifiers :cast-speed]
   :apply   #(+ (or %1 1) (/ %2 100))
   :reverse #(- %1 (/ %2 100))})

(modifier/defmodifier :modifiers/attack-speed
  {:values  [[15 25] [35 45] [50 60]]
   :text    (fn [v entity] (str "+" v "% Attack-Speed"))
   :keys    [:modifiers :attack-speed]
   :apply   #(+ (or %1 1) (/ %2 100))
   :reverse #(- %1 (/ %2 100))})

(defn- check-damage-block-modifier-value [[source-or-target
                                           damage-type
                                           value-delta]]
  (and (#{:source :target} source-or-target)
       (#{:physical :magic} damage-type)))

; TODO make shield or armor part of the modifier data ...
; -> only 1 modifier then with :shield or :armor as first value.
(defn- damage-block-modifier [block-type]
  {:text (fn [value _]
           (assert (check-damage-block-modifier-value value)
                   (str "Wrong value for modifier: " value))
           (str value))
   :keys [:modifiers block-type]
   :apply (fn [component value]
            (assert (check-damage-block-modifier-value value)
                    (str "Wrong value for shield/armor modifier: " value))
            (update-in component (drop-last value) #(+ (or % 0) (last value))))
   :reverse (fn [component value]
              (update-in component (drop-last value) - (last value)))})

; Example: [:shield [:target :physical 0.3]]
(modifier/defmodifier :modifiers/shield
  (damage-block-modifier :shield))

(modifier/defmodifier :modifiers/armor
  (damage-block-modifier :armor))

; TODO just use a spec  !
; or malli
; => I also want to edit it later @ property-editor
; hmm
(defn- check-damage-modifier-value [[source-or-target
                                     damage-type
                                     application-type
                                     value-delta]]
  (and (#{:source :target} source-or-target)
       (#{:physical :magic} damage-type)
       (let [[val-or-max inc-or-mult] application-type] ; TODO this is schema for val-max-modifiers !
         (and (#{:val :max} val-or-max)
              (#{:inc :mult} inc-or-mult)))))

(defn- default-value [application-type] ; TODO here too !
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
         " received ")))

; example: [:damage [:source :physical [:max :mult] 3]]
; TODO => effect-modifier-modifier
(modifier/defmodifier :modifiers/damage
  {:text (fn [value _]
           (assert (check-damage-modifier-value value)
                   (str "Wrong value for damage modifier: " value))
           (damage-modifier-text value))
   :keys [:modifiers :damage] ; :components/modifiers & :effects/damage
   :apply (fn [component value]
            (assert (check-damage-modifier-value value)
                    (str "Wrong value for damage modifier: " value))
            (update-in component (drop-last value) #(+ (or % (default-value (get value 2)))
                                                       (last value))))
   :reverse (fn [component value]
              (assert (check-damage-modifier-value value)
                      (str "Wrong value for damage modifier: " value))
              (update-in component (drop-last value) - (last value)))})
