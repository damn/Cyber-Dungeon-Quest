(ns cdq.tx.damage
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [apply-val apply-val-max-modifiers]]
            [utils.random :as random]
            [cdq.context :refer [transact!]]
            [cdq.effect :as effect]))

; make local fns
(defn- armor-save   [entity* damage-type] (-> entity* :entity/stats :stats/armor-save   damage-type))
(defn- armor-pierce [entity* damage-type] (-> entity* :entity/stats :stats/armor-pierce damage-type))

(defn- effective-armor-save [source target damage-type]
  (max (- (or (armor-save   target damage-type) 0)
          (or (armor-pierce source damage-type) 0))
       0))

(comment
 (let [armor-pierce 0.4
       armor-save 0.5
       source {:entity/stats {:stats/armor-pierce {:physical armor-pierce}}}
       target {:entity/stats {:stats/armor-save   {:physical armor-save}}}]
   [(armor-save   target :physical)
    (armor-pierce source :physical)
    (effective-armor-save source target :physical)])
 )

(defn- apply-damage-modifiers [{:keys [damage/min-max] :as damage}
                               modifiers]
  (if modifiers
    (update damage :damage/min-max apply-val-max-modifiers modifiers)
    damage))

(comment
 (= (apply-damage-modifiers {:damage/type :physical :damage/min-max [5 10]}
                            {[:val :inc] 3})
    #:damage{:type :physical, :min-max [8 10]})
 )

(defn- damage-stats [entity*]
  (-> entity* :entity/stats :stats/damage))

(defn- apply-source-modifiers [{:keys [damage/type] :as damage} source]
  (apply-damage-modifiers damage (-> source damage-stats :damage/deal type)))

(defn- apply-target-modifiers [{:keys [damage/type] :as damage} target]
  (apply-damage-modifiers damage (-> target damage-stats :damage/receive type)))

(comment
 (= (apply-source-modifiers {:damage/type :physical :damage/min-max [5 10]}
                            {:entity/stats {:stats/damage {:damage/deal {:physical {[:val :inc] 1}}}}})
    #:damage{:type :physical, :min-max [6 10]})

 (= (apply-source-modifiers {:damage/type :magic :damage/min-max [5 10]}
                            {:entity/stats {:stats/damage {:damage/deal {:physical {[:val :inc] 1}}}}})
    #:damage{:type :magic , :min-max [5 10]})

 (= (apply-source-modifiers {:damage/type :magic :damage/min-max [5 10]}
                            {:entity/stats {:stats/damage {:damage/deal {:magic {[:max :mult] 3}}}}})
    #:damage{:type :magic, :min-max [5 30]})
 )

(defn- effective-damage
  ([damage source]
   (-> damage
       (apply-source-modifiers source)))
  ([damage source target]
   (-> damage
       (apply-source-modifiers source)
       (apply-target-modifiers target))))

(comment
 (= (apply-damage-modifiers {:damage/type :physical :damage/min-max [3 10]}
                            {[:max :mult] 2
                             [:val :mult] 1.5
                             [:val :inc] 1
                             [:max :inc] 0})
    #:damage{:type :physical, :min-max [6 20]})

 (= (apply-damage-modifiers {:damage/type :physical :damage/min-max [6 20]}
                            {[:max :mult] 1
                             [:val :mult] 1
                             [:val :inc] -5
                             [:max :inc] 0})
    #:damage{:type :physical, :min-max [1 20]})

 (= (effective-damage {:damage/type :physical :damage/min-max [3 10]}
                      {:entity/stats {:stats/damage {:damage/deal {:physical {[:max :mult] 2
                                                                              [:val :mult] 1.5
                                                                              [:val :inc] 1
                                                                              [:max :inc] 0}}}}}
                      {:entity/stats {:stats/damage {:damage/receive {:physical {[:max :mult] 1
                                                                                 [:val :mult] 1
                                                                                 [:val :inc] -5
                                                                                 [:max :inc] 0}}}}})
    #:damage{:type :physical, :min-max [1 20]})
 )

(defn- saves? [armor-save]
  (< (rand) armor-save))

(defn- no-hp-left? [hp]
  (zero? (hp 0)))

(defn- damage->text [{:keys [damage/type] [min-dmg max-dmg] :damage/min-max}]
  (str min-dmg "-" max-dmg " " (name type) " damage"))

(defcomponent :tx/damage {:keys [damage/type] :as damage}
  (effect/text [_ {:keys [effect/source]}]
    (if source
      (let [modified (effective-damage damage @source)]
        (if (= damage modified)
          (damage->text damage)
          (str (damage->text damage) "\nModified: " (damage->text modified))))
      (damage->text damage))) ; property menu no source,modifiers

  (effect/valid-params? [_ {:keys [effect/source effect/target]}]
    (and source target))

  (transact! [_ {:keys [effect/source effect/target]}]
    (let [source* @source
          {:keys [entity/position entity/hp] :as target*} @target]
      (cond
       (not hp)
       []

       (no-hp-left? hp)
       []

       (saves? (effective-armor-save source* target* type))
       [[:tx/add-text-effect target "[WHITE]ARMOR"]]

       :else
       (let [{:keys [damage/type damage/min-max]} (effective-damage damage source* target*)
             dmg-amount (random/rand-int-between min-max)
             hp (apply-val hp #(- % dmg-amount))]
         [[:tx/audiovisual position (keyword (str "effects.damage." (name type)) "hit-effect")]
          [:tx/add-text-effect target (str "[RED]" dmg-amount)]
          [:tx/assoc target :entity/hp hp]
          [:tx/event target (if (no-hp-left? hp) :kill :alert)]])))))
