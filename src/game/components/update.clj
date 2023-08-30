(ns game.components.update
  (:require [game.components.modifiers :as modifiers]))

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
   :text     #(str (Math/signum (float (% 1))) (% 1) "% " (% 0))
   :apply    modify-update-speed
   :reverse  #(modify-update-speed %1 [(%2 0) (- (%2 1))])})
