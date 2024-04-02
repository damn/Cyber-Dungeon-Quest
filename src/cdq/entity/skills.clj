(ns cdq.entity.skills
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [apply-val]]
            [cdq.context :refer [get-property valid-params? ->counter stopped?]]
            [cdq.entity :as entity]))

; FIXME starting skills do not trigger tx/actionbar-add-skill
(defcomponent :entity/skills skills
  (entity/create-component [_ _components ctx]
    (zipmap skills (map #(get-property ctx %) skills)))

  (entity/tick [[k _] entity* ctx]
    (for [{:keys [property/id skill/cooling-down?]} (vals skills)
          :when (and cooling-down?
                     (stopped? ctx cooling-down?))]
      [:tx/assoc-in (:entity/id entity*) [k id :skill/cooling-down?] false])))

(extend-type cdq.entity.Entity
  entity/Skills
  (has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
    (contains? skills id)))

(defmethod cdq.context/transact! :tx/add-skill [[_ entity {:keys [property/id] :as skill}]
                                                _ctx]
  (assert (not (entity/has-skill? @entity skill)))
  [[:tx/assoc-in entity [:entity/skills id] skill]
   (when (:entity/player? @entity)
     [:tx/actionbar-add-skill skill])])

; unused ?
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
                       {:keys [skill/cost skill/cooling-down? skill/effect]}]
    (cond
     cooling-down?                               :cooldown
     (and cost (> cost (mana 0)))                :not-enough-mana
     (not (valid-params? effect-context effect)) :invalid-params
     :else                                       :usable)))
