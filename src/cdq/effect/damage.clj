(ns cdq.effect.damage
  (:require [malli.core :as m]
            [data.val-max :refer [apply-val apply-val-max-modifiers val-max-schema]]
            [utils.random :as random]
            [cdq.effect :as effect]
            [cdq.context :refer [send-event!]]
            [cdq.entity :as entity]))

(defn- source-block-ignore [entity* block-type damage-type]
  (-> entity* (entity/effect-source-modifiers block-type) damage-type))

(defn- target-block-rate [entity* block-type damage-type]
  (-> entity* (entity/effect-target-modifiers block-type) damage-type))

(defn- effective-block-rate [source* target* block-type damage-type]
  (max (- (or (target-block-rate   target* block-type damage-type) 0)
          (or (source-block-ignore source* block-type damage-type) 0))
       0))

(comment
 (let [block-ignore 0.4
       block-rate 0.5
       source {:entity/modifiers {:shield {:effect/source {:physical block-ignore}}}}
       target {:entity/modifiers {:shield {:effect/target {:physical block-rate}}}}]
   (effective-block-rate source target :shield :physical))
 )

(defn- apply-damage-modifiers [[damage-type min-max] modifiers]
  [damage-type (if modifiers
                 (apply-val-max-modifiers min-max modifiers)
                 min-max)])

(comment
 (apply-damage-modifiers [:physical [5 10]]
                         {[:val :inc] 3})
 [:physical [8 10]]
 )

(defn- apply-source-modifiers [{damage-type 0 :as damage} source*]
  (apply-damage-modifiers damage
                          (-> source* (entity/effect-source-modifiers :effect/damage) damage-type)))

(defn- apply-target-modifiers [{damage-type 0 :as damage} target*]
  (apply-damage-modifiers damage
                          (-> target* (entity/effect-target-modifiers :effect/damage) damage-type)))

(comment
 (set! *print-level* nil)
 (apply-source-modifiers [:physical [5 10]]
                         (cdq.entity/map->Entity
                          {:entity/modifiers {:effect/damage {:effect/source {:physical {[:val :inc] 1}}}}}))
 [:physical [6 10]]
 (apply-source-modifiers [:magic [5 10]]
                         (cdq.entity/map->Entity
                          {:entity/modifiers {:effect/damage {:effect/source {:physical {[:val :inc] 1}}}}}))
 [:magic [5 10]]

 (apply-source-modifiers [:magic [5 10]]
                         (cdq.entity/map->Entity
                          {:entity/modifiers {:effect/damage {:effect/source {:magic {[:max :mult] 3}}}}}))
 [:magic [5 30]]
 )

(defn- effective-damage
  ([damage source*]
   (-> damage
       (apply-source-modifiers source*)))
  ([damage source* target*]
   (-> damage
       (apply-source-modifiers source*)
       (apply-target-modifiers target*))))

(comment
 (apply-damage-modifiers [:physical [3 10]]
                         {[:max :mult] 2
                          [:val :mult] 1.5
                          [:val :inc] 1
                          [:max :inc] 0})
 [:physical [6 20]]
 (apply-damage-modifiers [:physical [6 20]]
                         {[:max :mult] 1
                          [:val :mult] 1
                          [:val :inc] -5
                          [:max :inc] 0})
 [:physical [1 20]]

 (effective-damage [:physical [3 10]]
                   (cdq.entity/map->Entity
                    {:entity/modifiers {:effect/damage {:effect/source {:physical  {[:max :mult] 2
                                                                                    [:val :mult] 1.5
                                                                                    [:val :inc] 1
                                                                                    [:max :inc] 0}}}}})
                   (cdq.entity/map->Entity
                    {:entity/modifiers {:effect/damage {:target {:physical  {[:max :mult] 1
                                                                             [:val :mult] 1
                                                                             [:val :inc] -5
                                                                             [:max :inc] 0}}}}}))
 [:physical [1 20]]
 )

(defn- shield-blocked-tx [context entity*]
  (entity/add-text-effect entity* context "SHIELD"))

(defn- armor-blocked-tx [context entity*]
  (entity/add-text-effect entity* context "ARMOR"))

(defn- blocks? [block-rate]
  (< (rand) block-rate))

(defn- no-hp-left? [hp]
  (zero? (hp 0)))

(defn- damage->text [[dmg-type [min-dmg max-dmg]]]
  (str min-dmg "-" max-dmg " " (name dmg-type) " damage"))

(def ^:private damage-schema
  (m/schema [:tuple [:enum :physical :magic] (m/form val-max-schema)]))

(defmethod effect/value-schema :effect/damage [_]
  damage-schema)

(defmethod effect/text :effect/damage
  [{:keys [effect/source]} [_ damage]]
  (if source
    (let [modified (effective-damage damage @source)]
      (if (= damage modified)
        (damage->text damage)
        (str (damage->text damage) "\nModified: " (damage->text modified))))
    (damage->text damage))) ; property menu no source,modifiers

(defmethod effect/valid-params? :effect/damage
  [{:keys [effect/source effect/target]} _effect]
  (and source target (:entity/hp @target)))

(defmethod effect/transactions :effect/damage
  [{:keys [effect/source
           effect/target] :as context} [_ {dmg-type 0 :as damage}]]
  (cond
   (no-hp-left? (:entity/hp @target))
   nil

   (blocks? (effective-block-rate @source @target :shield dmg-type))
   [(shield-blocked-tx context @target)]

   (blocks? (effective-block-rate @source @target :armor dmg-type))
   [(armor-blocked-tx context @target)]

   :else
   (let [[dmg-type min-max-dmg] (effective-damage damage @source @target)
         dmg-amount (random/rand-int-between min-max-dmg)]
     [[:tx/audiovisual (:entity/position @target) (keyword (str "effects.damage." (name dmg-type)) "hit-effect")]
      (-> @target
          (entity/add-text-effect context (str "[RED]" dmg-amount))
          (update :entity/hp apply-val #(- % dmg-amount)))
      [:tx/event target (if (no-hp-left? (:entity/hp @target)) :kill :alert)]])))
