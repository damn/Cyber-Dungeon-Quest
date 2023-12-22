(ns context.entity.skills
  (:require [x.x :refer [defcomponent]]
            [data.counter :as counter]
            [utils.core :refer [mapvals]]
            [context.entity :as entity]
            [game.context :refer [get-property valid-params?]]
            game.entity))

(defn- update-cooldown [skill delta]
  (if (:cooling-down? skill)
    (update skill :cooling-down?
            #(let [counter (counter/tick % delta)]
               (if (counter/stopped? counter)
                 false
                 counter)))
    skill))

(defcomponent :skills skills
  (entity/create! [_ entity context]
    (swap! entity update :skills
           #(zipmap %
                    (map (fn [skill-id] (get-property context skill-id))
                         %))))
  (entity/tick [_ delta]
    (mapvals #(update-cooldown % delta) skills)))

(extend-type context.entity.Entity
  game.entity/Skills
  (has-skill? [{:keys [skills]} {:keys [id]}]
    (contains? skills id))

  (add-skill [entity* {:keys [id] :as skill}]
    (assert (not (game.entity/has-skill? entity* skill)))
    (update entity* :skills assoc id skill))

  (remove-skill [entity* {:keys [id] :as skill}]
    (assert (game.entity/has-skill? entity* skill))
    (update entity* :skills dissoc id))

  (set-skill-to-cooldown [entity* {:keys [id cooldown] :as skill}]
    (if cooldown
      (assoc-in entity* [:skills id :cooling-down?] (counter/create cooldown))
      entity*)))

(defn usable-state [effect-context
                    {:keys [mana]}
                    {:keys [cost cooling-down? effect]}]
  (cond
   cooling-down?                               :cooldown
   (and cost (> cost (mana 0)))                :not-enough-mana
   (not (valid-params? effect-context effect)) :invalid-params
   :else                                       :usable))
