(ns context.entity.skills
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [mapvals]]
            [data.val-max :refer [apply-val]]
            [context.entity :as entity]
            [cdq.context :refer [get-property valid-params? ->counter stopped?
                                  actionbar-add-skill actionbar-remove-skill]]
            cdq.entity))

(defn- update-cooldown [context skill]
  (if (:cooling-down? skill)
    (update skill :cooling-down?
            (fn [counter]
              (if (stopped? context counter)
                false
                counter)))
    skill))

(defcomponent :skills skills
  (entity/create! [[k skill-ids] entity context]
    (swap! entity assoc k (zipmap skill-ids
                                  (map #(get-property context %)
                                       skill-ids))))

  (entity/tick! [[k _] entity context]
    (swap! entity update k
           (fn [skills]
             (mapvals #(update-cooldown context %) skills)))))

(extend-type context.entity.Entity
  cdq.entity/Skills
  (has-skill? [{:keys [skills]} {:keys [property/id]}]
    (contains? skills id)))

(extend-type gdl.context.Context
  cdq.context/Skills
  (add-skill! [ctx entity {:keys [property/id] :as skill}]
    (assert (not (cdq.entity/has-skill? @entity skill)))
    (swap! entity update :skills assoc id skill)
    (when (:is-player @entity)
      (actionbar-add-skill ctx skill)))

  (remove-skill! [ctx entity {:keys [property/id] :as skill}]
    (assert (cdq.entity/has-skill? @entity skill))
    (swap! entity update :skills dissoc id)
    (when (:is-player @entity)
      (actionbar-remove-skill ctx skill)))

  (set-skill-to-cooldown! [context entity {:keys [property/id skill/cooldown] :as skill}]
    (when cooldown
      (swap! entity assoc-in [:skills id :cooling-down?] (->counter context cooldown))))

  (skill-usable-state [effect-context
                       {:keys [mana]}
                       {:keys [skill/cost cooling-down? skill/effect]}]
    (cond
     cooling-down?                               :cooldown
     (and cost (> cost (mana 0)))                :not-enough-mana
     (not (valid-params? effect-context effect)) :invalid-params
     :else                                       :usable))

  (pay-skill-mana-cost! [_ entity skill]
    (swap! entity (fn [entity*]
                    (if (:skill/cost skill)
                      (update entity* :mana apply-val #(- % (:skill/cost skill)))
                      entity*)))))
