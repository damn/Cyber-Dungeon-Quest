(ns context.skill-info
  (:require [clojure.set :as set]
            [clojure.string :as str]
            gdl.context
            [utils.core :refer [readable-number]]
            [cdq.context :refer [effect-text]]))

(defn- ms->pprint-seconds [ms]
  (readable-number (/ ms 1000)))

(extend-type gdl.context.Context
  cdq.context/SkillInfo
  (skill-text [{:keys [context/player-entity] :as context}
               {:keys [id
                       cost
                       action-time
                       cooldown
                       spell?
                       effect]}]
              (str (str/capitalize (name id)) "\n"
                   (if spell? "Spell" "Weapon") "\n"
                   (when cost (str "Cost " cost  "\n"))
                   (if spell?  "Cast-Time " "Attack-time ") (ms->pprint-seconds action-time) " seconds\n"
                   (when cooldown (str "Cooldown " (ms->pprint-seconds cooldown) "\n"))
                   (effect-text (merge context {:effect/source player-entity})
                                effect))))
