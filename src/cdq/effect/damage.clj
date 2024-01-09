(ns cdq.effect.damage
  (:require [x.x :refer [defcomponent]]
            [data.val-max :refer [apply-val apply-val-max-modifiers]]
            [utils.random :as random]
            [cdq.effect :as effect]
            [cdq.entity :as entity]))

(defn- source-block-ignore [entity* block-type damage-type]
  (-> entity* (entity/effect-source-modifiers block-type) damage-type))

(defn- target-block-rate [entity* block-type damage-type]
  (-> entity* (entity/effect-target-modifiers block-type) damage-type))

(defn- effective-block-rate [source target block-type damage-type]
  (max (- (or (target-block-rate   target block-type damage-type) 0)
          (or (source-block-ignore source block-type damage-type) 0))
       0))

(comment
 (let [block-ignore 0.4
       block-rate 0.5
       source {:entity/modifiers {:shield {:effect/source {:physical block-ignore}}}}
       target {:entity/modifiers {:shield {:effect/target {:physical block-rate}}}}]
   (effective-block-rate source target :shield :physical))
 )

(defn- apply-damage-modifiers [{:keys [damage/min-max] :as damage}
                               modifiers]
  (if modifiers
    (update damage :damage/min-max apply-val-max-modifiers modifiers)
    damage))

(comment
 (apply-damage-modifiers {:damage/type :physical :damage/min-max [5 10]}
                         {[:val :inc] 3})
 #:damage{:type :physical, :min-max [8 10]}
 )

(defn- apply-source-modifiers [{:keys [damage/type] :as damage} source]
  (apply-damage-modifiers damage
                          (-> source (entity/effect-source-modifiers :effect/damage) type)))

(defn- apply-target-modifiers [{:keys [damage/type] :as damage} target]
  (apply-damage-modifiers damage
                          (-> target (entity/effect-target-modifiers :effect/damage) type)))

(comment
 (set! *print-level* nil)
 (apply-source-modifiers {:damage/type :physical :damage/min-max [5 10]}
                         (cdq.entity/map->Entity
                          {:entity/modifiers {:effect/damage {:effect/source {:physical {[:val :inc] 1}}}}}))
 #:damage{:type :physical, :min-max [6 10]}

 (apply-source-modifiers {:damage/type :magic :damage/min-max [5 10]}
                         (cdq.entity/map->Entity
                          {:entity/modifiers {:effect/damage {:effect/source {:physical {[:val :inc] 1}}}}}))
 #:damage{:type :magic, :min-max [5 10]}

 (apply-source-modifiers {:damage/type :magic :damage/min-max [5 10]}
                         (cdq.entity/map->Entity
                          {:entity/modifiers {:effect/damage {:effect/source {:magic {[:max :mult] 3}}}}}))
 #:damage{:type :magic, :min-max [5 30]}
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
 (apply-damage-modifiers {:damage/type :physical :damage/min-max [3 10]}
                         {[:max :mult] 2
                          [:val :mult] 1.5
                          [:val :inc] 1
                          [:max :inc] 0})
 #:damage{:type :physical, :min-max [6 20]}

 (apply-damage-modifiers {:damage/type :physical :damage/min-max [6 20]}
                         {[:max :mult] 1
                          [:val :mult] 1
                          [:val :inc] -5
                          [:max :inc] 0})
 #:damage{:type :physical, :min-max [1 20]}

 (effective-damage {:damage/type :physical :damage/min-max [3 10]}
                   (cdq.entity/map->Entity
                    {:entity/modifiers {:effect/damage {:effect/source {:physical {[:max :mult] 2
                                                                                   [:val :mult] 1.5
                                                                                   [:val :inc] 1
                                                                                   [:max :inc] 0}}}}})
                   (cdq.entity/map->Entity
                    {:entity/modifiers {:effect/damage {:effect/target {:physical {[:max :mult] 1
                                                                                   [:val :mult] 1
                                                                                   [:val :inc] -5
                                                                                   [:max :inc] 0}}}}}))
 #:damage{:type :physical, :min-max [1 20]}
 )

(defn- blocks? [block-rate]
  (< (rand) block-rate))

(defn- no-hp-left? [hp]
  (zero? (hp 0)))

(defn- damage->text [{:keys [damage/type] [min-dmg max-dmg] :damage/min-max}]
  (str min-dmg "-" max-dmg " " (name type) " damage"))

(defcomponent :effect/damage {:keys [damage/type] :as damage}
  (effect/text [_ {:keys [effect/source]}]
    (if source
      (let [modified (effective-damage damage source)]
        (if (= damage modified)
          (damage->text damage)
          (str (damage->text damage) "\nModified: " (damage->text modified))))
      (damage->text damage))) ; property menu no source,modifiers

  (effect/valid-params? [_ {:keys [effect/source effect/target]}]
    (and source target (:entity/hp target)))

  (effect/transactions [_ {:keys [effect/source effect/target]
                           {:keys [entity/position
                                   entity/id
                                   entity/hp]} :effect/target}]
    (cond
     (no-hp-left? hp)
     nil

     (blocks? (effective-block-rate source target :shield type))
     [[:tx/add-text-effect id "[WHITE]SHIELD"]]

     (blocks? (effective-block-rate source target :armor type))
     [[:tx/add-text-effect id "[WHITE]ARMOR"]]

     :else
     (let [{:keys [damage/type damage/min-max]} (effective-damage damage source target)
           dmg-amount (random/rand-int-between min-max)
           hp (apply-val hp #(- % dmg-amount))]
       [[:tx/audiovisual position (keyword (str "effects.damage." (name type)) "hit-effect")]
        [:tx/add-text-effect id (str "[RED]" dmg-amount)]
        [:tx/assoc id :entity/hp hp]
        [:tx/event id (if (no-hp-left? hp) :kill :alert)]]))))
