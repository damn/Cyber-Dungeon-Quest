(ns context.effect.damage
  (:require [data.val-max :refer [apply-val apply-val-max-modifiers]]
            [utils.random :as random]
            [context.effect :as effect]
            [cdq.context :refer [audiovisual send-event! add-text-effect!]]
            [cdq.entity :as entity]))

; example:
; [:effect/damage [:physical [5 6]]]
(comment
 ; malli:
 [:tuple [:enum :effect/damage]
  [:tuple [:enum :physical :magic]
   [:tuple int? int?]]]
 ; TODO => val-max-data as schema!
 ; two >=0 integers and val<=max.
 )

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
       source {:modifiers {:shield {:effect/source {:physical block-ignore}}}}
       target {:modifiers {:shield {:effect/target {:physical block-rate}}}}]
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
                         (context.entity/map->Entity
                          {:modifiers {:effect/damage {:effect/source {:physical {[:val :inc] 1}}}}}))
 [:physical [6 10]]
 (apply-source-modifiers [:magic [5 10]]
                         (context.entity/map->Entity
                          {:modifiers {:effect/damage {:effect/source {:physical {[:val :inc] 1}}}}}))
 [:magic [5 10]]

 (apply-source-modifiers [:magic [5 10]]
                         (context.entity/map->Entity
                          {:modifiers {:effect/damage {:effect/source {:magic {[:max :mult] 3}}}}}))
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
                   (context.entity/map->Entity
                    {:modifiers {:effect/damage {:effect/source {:physical  {[:max :mult] 2
                                                                             [:val :mult] 1.5
                                                                             [:val :inc] 1
                                                                             [:max :inc] 0}}}}})
                   (context.entity/map->Entity
                    {:modifiers {:effect/damage {:target {:physical  {[:max :mult] 1
                                                                      [:val :mult] 1
                                                                      [:val :inc] -5
                                                                      [:max :inc] 0}}}}}))
 [:physical [1 20]]
 )

(defn- shield-blocked-effect [context entity]
  (add-text-effect! context entity "SHIELD"))

(defn- armor-blocked-effect [context entity]
  (add-text-effect! context entity "ARMOR"))

(defn- blocks? [block-rate]
  (< (rand) block-rate))

(defn- no-hp-left? [hp]
  (zero? (hp 0)))

(defn- damage->text [[dmg-type [min-dmg max-dmg]]]
  (str min-dmg "-" max-dmg " " (name dmg-type) " damage"))

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
  (and source target (:hp @target)))

(defmethod effect/do! :effect/damage
  [{:keys [effect/source
           effect/target] :as context} [_ {dmg-type 0 :as damage}]]
  (cond
   (no-hp-left? (:hp @target))
   nil

   (blocks? (effective-block-rate @source @target :shield dmg-type))
   (shield-blocked-effect context target)

   (blocks? (effective-block-rate @source @target :armor dmg-type))
   (armor-blocked-effect context target)

   :else
   (let [[dmg-type min-max-dmg] (effective-damage damage @source @target)
         dmg-amount (random/rand-int-between min-max-dmg)]
     (audiovisual context (:position @target)
                  (keyword (str "effects.damage." (name dmg-type))
                           "hit-effect"))
     (swap! target update :hp apply-val #(- % dmg-amount))
     (add-text-effect! context target (str "[RED]" dmg-amount))
     (send-event! context target
                  (if (no-hp-left? (:hp @target))
                    :kill
                    :alert)))))
