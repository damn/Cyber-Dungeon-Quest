; TODO all modifier-fns broken, need entity arg ( only skill has)
; TODO parse valid modifiers @ read properties !
; schema .. !
(ns game.modifier)

(def modifier-definitions {})

(defn defmodifier [k modifier] ; TODO just 'def'
  (alter-var-root #'modifier-definitions assoc k modifier)
  k)

; Example:
; (call-modifier-fn :apply entity* [:mana 30])
(defn- call-modifier-fn [fn-k entity* [modifier-key modifier-value]]
  {:pre [(#{:apply :reverse} fn-k)]}
  (let [modifier-def (get modifier-definitions modifier-key)
        modify (fn-k modifier-def)]
    (assert modifier-def (str "Could not find modifier: " modifier-key))
    (update-in entity* (:keys modifier-def) modify modifier-value)))

(defn- call-modifier-fns [entity* fn-k modifiers]
  (reduce (partial call-modifier-fn fn-k)
          entity*
          modifiers))

(defn apply-modifiers [entity* modifiers]
  (call-modifier-fns entity* :apply modifiers))

(defn reverse-modifiers [entity* modifiers]
  (call-modifier-fns entity* :reverse modifiers))

(defn text [entity [modifier-type value]]
  ((:text (modifier-type modifier-definitions))
   value
   entity))
