(ns cdq.context.modifier
  (:require [clojure.string :as str]
            gdl.context
            cdq.context))

(def modifier-definitions {})

(defn defmodifier [k modifier]
  (alter-var-root #'modifier-definitions assoc k modifier)
  k)

; TODO assert, do like effect

; Example:
; (call-modifier-fn :apply entity* [:mana 30])
(defn- call-modifier-fn [fn-k entity* [modifier-key modifier-value]]
  {:pre [(#{:apply :reverse} fn-k)]}
  (let [modifier-def (get modifier-definitions modifier-key)
        modify (fn-k modifier-def)]
    (assert modifier-def (str "Could not find modifier: " modifier-key))
    (update-in entity* (:keys modifier-def) modify modifier-value)))

(defn- reduce-modifier [entity* fn-k modifier]
  (reduce (partial call-modifier-fn fn-k)
          entity*
          modifier))

(defn- apply-modifier   [entity* modifier] (reduce-modifier entity* :apply   modifier))
(defn- reverse-modifier [entity* modifier] (reduce-modifier entity* :reverse modifier))

(defn- text [[modifier-type value]]
  ((:text (modifier-type modifier-definitions))
   value))

; TODO modifiers without reduce
; => just effect with an reverse! function
; calling doseq ! ?
; but I like reduce & simple function
; why does everything have to be with side effects ?
; weil man darauf reagieren muss im falle hp
; im falle mana vlt auch , flash manabar
; speed/damage/block modifier idk
; could flash the stats labels in stats window below inventory
; => a way to organise/data based define swap! entity 'effects'
; can I reuse this in other swap entity effects ?
; [:mark-for-removal! entity]
; data based side effect language ? possible ?
; where all are these ctx fns called
; set/remove item is a 'modifier' itself o.o

; => don't reduce two reasons:
; * I can REACT to it
; * permanent blessings/curses which show small icon somewhere or component drawing with tooltip which will
; not be reversed, => use them as effects
; e.g. curse your opponents => dark energy => -50% HP, +50% strength.
; * similar to effects - text ...

(extend-type gdl.context.Context
  cdq.context/Modifier
  (apply-modifier! [_ entity modifier]
    (swap! entity apply-modifier modifier))

  (reverse-modifier! [_ entity modifier]
    (swap! entity reverse-modifier modifier))

  (modifier-text [_ modifier]
    (->> (for [component modifier]
           (text component))
         (str/join "\n"))))
