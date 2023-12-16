(ns game.skill
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [utils.core :refer [readable-number]]
            [game.effect :as effect]))

; TODO  prepare-properties :skills
; add :image32x32 (g/get-scaled-copy image [32 32])
; -> use @ draw-skill-icon
; fix icon size (weapons have item 48x48 and spells 32x32)

; TODO check-properties

(def ^:private property-keys
  #{:cost
    :action-time
    :cooldown
    :spell?
    :image
    :effect})

(defn- check-properties [properties]
  (let [keyset (set (keys properties))]
    (assert (set/subset? property-keys keyset)
            (str "Skill properties are missing keys: " keyset))))

(defn- ms->pprint-seconds [ms]
  (readable-number (/ ms 1000)))

(defn text [{:keys [id
                    cost
                    action-time
                    cooldown
                    spell?
                    effect]}
            entity]
  (str (str/capitalize (name id)) "\n"
       (if spell? "Spell" "Weapon") "\n"
       (when cost (str "Cost " cost  "\n"))
       (if spell?  "Cast-Time " "Attack-time ") (ms->pprint-seconds action-time) " seconds\n"
       (when cooldown (str "Cooldown " (ms->pprint-seconds cooldown) "\n"))
       (effect/text effect {:source entity})))
