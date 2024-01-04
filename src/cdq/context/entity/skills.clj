(ns cdq.context.entity.skills
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [mapvals]]
            [data.val-max :refer [apply-val]]
            [cdq.context.ecs :as ecs]
            [cdq.context :refer [valid-params? ->counter stopped? actionbar-add-skill actionbar-remove-skill]]
            [cdq.entity :as entity]))

(defn- update-cooldown [context skill]
  (if (:cooling-down? skill)
    (update skill :cooling-down?
            (fn [counter]
              (if (stopped? context counter)
                false
                counter)))
    skill))

(defcomponent :entity/skills skills
  (ecs/tick! [[k _] entity context]
    (swap! entity update k (fn [skills]
                             (mapvals #(update-cooldown context %) skills)))))

(extend-type cdq.context.ecs.Entity
  cdq.entity/Skills
  (has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
    (contains? skills id)))

(extend-type gdl.context.Context
  cdq.context/Skills
  (add-skill! [ctx entity {:keys [property/id] :as skill}]
    (assert (not (entity/has-skill? @entity skill)))
    (swap! entity update :entity/skills assoc id skill)
    (when (:entity/player? @entity)
      (actionbar-add-skill ctx skill)))

  (remove-skill! [ctx entity {:keys [property/id] :as skill}]
    (assert (entity/has-skill? @entity skill))
    (swap! entity update :entity/skills dissoc id)
    (when (:entity/player? @entity)
      (actionbar-remove-skill ctx skill)))

  (set-skill-to-cooldown! [context entity {:keys [property/id skill/cooldown] :as skill}]
    (when cooldown
      (swap! entity assoc-in [:entity/skills id :cooling-down?] (->counter context cooldown))))

  (skill-usable-state [effect-context
                       {:keys [entity/mana]}
                       {:keys [skill/cost cooling-down? skill/effect]}]
    (cond
     cooling-down?                               :cooldown
     (and cost (> cost (mana 0)))                :not-enough-mana
     (not (valid-params? effect-context effect)) :invalid-params
     :else                                       :usable))

  (pay-skill-mana-cost! [_ entity skill]
    (swap! entity (fn [entity*]
                    (if (:skill/cost skill)
                      (update entity* :entity/mana apply-val #(- % (:skill/cost skill)))
                      entity*)))))