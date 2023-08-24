; TODO everywhere we use modifier-definitions we ASSERT that the type was found
; otherwise throw an error -> same with skills/items/creatures/whatever
; TODO all modifier-fns broken, need entity arg ( only skill has)
(ns game.components.modifiers)

; TODO modifiers are also systems? (apply/reverse systems) / reversable transformer systems
; but modifier require 2 functions to be implemented .... hmm ?
; updating component/entity*/entity! ? ...
(def modifier-definitions {})

(defn defmodifier [k modifier]
  (alter-var-root #'modifier-definitions assoc k modifier)
  k)

; Example:
; (call-modifier-fn :apply entity* [:mana 30])
(defn- call-modifier-fn [fn-k entity* [modifier-key modifier-value]]
  {:pre [(#{:apply :reverse} fn-k)]}
  (let [modifier-def (get modifier-definitions modifier-key)
        modify (fn-k modifier-def)]
    (assert modifier-def (str "Could not find modifier: " modifier-key))
    (if-let [ks (:keys modifier-def)]
      (update-in entity* ks modify modifier-value)
      (modify entity* modifier-value))))

(defn- call-modifier-fns [entity* fn-k modifiers]
  (reduce (partial call-modifier-fn fn-k)
          entity*
          modifiers))

; TODO make apply and reverse pure ?
(defn apply! [entity modifiers]
  (swap! entity call-modifier-fns :apply modifiers))

(defn reverse! [entity modifiers]
  (swap! entity call-modifier-fns :reverse modifiers))

(defn text [entity [modifier-type value]]
  ((:text (modifier-type modifier-definitions))
   value
   entity))
