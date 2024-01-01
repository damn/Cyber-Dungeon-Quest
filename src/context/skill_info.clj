(ns context.skill-info
  (:require [clojure.set :as set]
            [clojure.string :as str]
            gdl.context
            [utils.core :refer [readable-number]]
            [cdq.context :refer [effect-text]]))

(extend-type gdl.context.Context
  cdq.context/SkillInfo
  (skill-text [{:keys [context/player-entity] :as context}
               {:keys [property/id cost action-time cooldown spell? effect]}]
    (str (str/capitalize (name id)) "\n"
         (if spell? "Spell" "Weapon") "\n"
         (when cost (str "Cost " cost  "\n"))
         (if spell?  "Cast-Time " "Attack-time ") (readable-number action-time) " seconds\n"
         (when cooldown (str "Cooldown " (readable-number cooldown) "\n"))
         (effect-text (merge context {:effect/source player-entity})
                      effect))))
