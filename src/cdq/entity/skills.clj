(ns cdq.entity.skills
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [mapvals]]
            [data.val-max :refer [apply-val]]
            [cdq.context :refer [valid-params? ->counter stopped?]]
            [cdq.entity :as entity]))

(defcomponent :entity/skills skills
  (entity/tick [[k _] entity* ctx]
    (for [{:keys [property/id cooling-down?]} (vals skills)
          :when (and cooling-down?
                     (stopped? ctx cooling-down?))]
      [:tx/assoc-in (:entity/id entity*) [k id :cooling-down?] false])))

(extend-type cdq.entity.Entity
  cdq.entity/Skills
  (has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
    (contains? skills id))

  (set-skill-to-cooldown [entity* context {:keys [property/id skill/cooldown] :as skill}]
    (if cooldown
      (assoc-in entity* [:entity/skills id :cooling-down?] (->counter context cooldown))
      entity*))

  (pay-skill-mana-cost [entity* skill]
    (if (:skill/cost skill)
      (update entity* :entity/mana apply-val #(- % (:skill/cost skill)))
      entity*)))

(defmethod cdq.context/transact! :tx/add-skill [[_ entity {:keys [property/id] :as skill}]
                                                _ctx]
  (assert (not (entity/has-skill? @entity skill)))
  [[:tx/assoc-in entity [:entity/skills id] skill]
   (when (:entity/player? @entity)
     [:tx/actionbar-add-skill skill])])

(defmethod cdq.context/transact! :tx/remove-skill [[_ entity {:keys [property/id] :as skill}]
                                                   _ctx]
  (assert (entity/has-skill? @entity skill))
  [[:tx/dissoc-in entity [:entity/skills id]]
   (when (:entity/player? @entity)
     [:tx/actionbar-remove-skill skill])])

(extend-type gdl.context.Context
  cdq.context/Skills
  (skill-usable-state [effect-context
                       {:keys [entity/mana]}
                       {:keys [skill/cost cooling-down? skill/effect]}]
    (cond
     cooling-down?                               :cooldown
     (and cost (> cost (mana 0)))                :not-enough-mana
     (not (valid-params? effect-context effect)) :invalid-params
     :else                                       :usable)))
