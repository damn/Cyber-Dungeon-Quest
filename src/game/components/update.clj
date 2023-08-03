(nsx game.components.update)

; x/components/modifier/update-speed
; x/components/modifer/blocks ; !!!!

; => game.components .tick/core
; also its modifiers only -> define modifiers also in separate ns?

; TODO define modifiers with their component or without?
; as I am always have to check 'default-value' or not ...

; TODO define modifiable-values as a data-structure
; e.g. movement-speed (moddable default-value)
; and it keeps record of stuff ?

; Best solution: component does not know it is being modified, just using 'attack-speed' ?
; or :attack-speed (modifiable 1)
; (-> v :attack-speed :current)

; {:default 1 :current 1}


;; TODO these are sub-components, components of components (modifiers are components of components ?)
; => other modifiers also cast-speed/attack-speed go to modifier component ?
; => block/update-speed also ?
; modifiers are components in a modifier  component ???

(defn- add-block [entity* ctype]
  (update-in entity* [ctype :blocks] #(inc (or % 0))))

(defn- remove-block [entity* ctype]
  {:pre  [(> (get-in entity* [ctype :blocks]) 0)]}
  (update-in entity* [ctype :blocks] dec))

(modifiers/defmodifier :block
  {:text     #(str "Stops " (name %))
   :apply    add-block
   :reverse  remove-block})

(defn- modify-update-speed [entity* [ctype value]]
  (update-in entity* [ctype :update-speed] #(+ (or % 1) value)))

(modifiers/defmodifier :update-speed
  {:values   [[8 20] [25 35] [40 50]]
   :text     #(str (Math/signum (% 1)) (% 1) "% " (% 0))
   :apply    modify-update-speed
   :reverse  #(modify-update-speed %1 [(%2 0) (- (%2 1))])})


; TODO 'multiplier' is-a 'modifier'
; movement only needs to know it has a 'speed'
; modifier checks if any modifier is on speed (:modifier in component value)
; otherwise saves base-value & adds multiplier
; if modifier multiplier is there, adds or subtracts from multiplier
; also cleanly removes ?
;
