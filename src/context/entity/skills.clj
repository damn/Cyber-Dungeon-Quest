(ns context.entity.skills
  (:require [x.x :refer [defcomponent]]
            [utils.core :refer [mapvals]]
            [data.val-max :refer [apply-val]]
            [context.entity :as entity]
            [game.context :refer [get-property valid-params? ->counter stopped?
                                  actionbar-add-skill actionbar-remove-skill]]
            game.entity))

(defn- update-cooldown [context skill delta]
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

  (entity/tick! [_ entity context delta]
    (mapvals #(update-cooldown context % delta) skills)))

(extend-type context.entity.Entity
  game.entity/Skills
  (has-skill? [{:keys [skills]} {:keys [id]}]
    (contains? skills id)))

(extend-type gdl.context.Context
  game.context/Skills
  (add-skill! [ctx entity {:keys [id] :as skill}]
    (assert (not (game.entity/has-skill? @entity skill)))
    (swap! entity update :skills assoc id skill)
    (when (:is-player @entity)
      (actionbar-add-skill ctx skill)))

  (remove-skill! [ctx entity {:keys [id] :as skill}]
    (assert (game.entity/has-skill? @entity skill))
    (swap! entity update :skills dissoc id)
    (when (:is-player @entity)
      (actionbar-remove-skill ctx skill)))

  (set-skill-to-cooldown! [context entity {:keys [id cooldown] :as skill}]
    (when cooldown
      (swap! entity assoc-in [:skills id :cooling-down?] (->counter context cooldown))))

  (skill-usable-state [effect-context
                       {:keys [mana]}
                       {:keys [cost cooling-down? effect]}]
    (cond
     cooling-down?                               :cooldown
     (and cost (> cost (mana 0)))                :not-enough-mana
     (not (valid-params? effect-context effect)) :invalid-params
     :else                                       :usable))

  (pay-skill-mana-cost! [_ entity skill]
    (swap! entity (fn [entity*]
                    (if (:cost skill)
                      (update entity* :mana apply-val #(- % (:cost skill)))
                      entity*)))))
