; TODO tests?
(ns game.effects.damage
  (:require [gdl.audio :as audio]
            [data.val-max :refer [apply-val-max-modifiers]]
            [game.utils.random :as random]
            [game.effect :as effect]
            [game.media :as media]
            [game.components.hp :refer [dead?]]
            [game.components.modifiers :refer [effect-source-modifiers effect-target-modifiers]]
            [game.components.string-effect :as string-effect]
            [game.entities.animation :as animation-entity]))

; example:
; [:damage [:physical [5 6]]]
(comment
 ; malli:
 [:tuple [:enum :damage]
  [:tuple [:enum :physical :magic]
   [:tuple int? int?]]]
 ; TODO => val-max-data as schema!
 ; two >=0 integers and val<=max.
 )

(defn- source-block-ignore [entity* block-type damage-type]
  (-> entity* (effect-source-modifiers block-type) damage-type))

(defn- target-block-rate [entity* block-type damage-type]
  (-> entity* (effect-target-modifiers block-type) damage-type))

(defn- effective-block-rate [source* target* block-type damage-type]
  (max (- (or (target-block-rate target* block-type damage-type) 0)
          (or (source-block-ignore source* block-type damage-type) 0))
       0))

(comment
 (let [block-ignore 0.4
       block-rate 0.5
       source {:modifiers {:shield {:source {:physical block-ignore}}}}
       target {:modifiers {:shield {:target {:physical block-rate}}}}]
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
                          (-> source* (effect-source-modifiers :damage) damage-type)))

(defn- apply-target-modifiers [{damage-type 0 :as damage} target*]
  (apply-damage-modifiers damage
                          (-> target* (effect-target-modifiers :damage) damage-type)))

(comment
 (set! *print-level* nil)
 (apply-source-modifiers [:physical [5 10]]
                         {:modifiers {:damage {:source {:physical {[:val :inc] 1}}}}})
 [:physical [6 10]]
 (apply-source-modifiers [:magic [5 10]]
                         {:modifiers {:damage {:source {:physical {[:val :inc] 1}}}}})
 [:magic [5 10]]

 (apply-source-modifiers [:magic [5 10]]
                         {:modifiers {:damage {:source {:magic {[:max :mult] 3}}}}})
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
                   {:modifiers {:damage {:source {:physical  {[:max :mult] 2
                                                              [:val :mult] 1.5
                                                              [:val :inc] 1
                                                              [:max :inc] 0}}}}}
                   {:modifiers {:damage {:target {:physical  {[:max :mult] 1
                                                              [:val :mult] 1
                                                              [:val :inc] -5
                                                              [:max :inc] 0}}}}})
 [:physical [1 20]]
 )

(defn- shield-blocked-effect [entity]
  (swap! entity string-effect/add "SHIELD"))

(defn- armor-blocked-effect [entity]
  ; (audio/play "bfxr_armorhit.wav")
  (swap! entity string-effect/add "ARMOR"))

(defn- dmg-type->hit-effect! [dmg-type position]
  (let [[sound fx-idx] (case dmg-type
                         :physical ["bfxr_normalhit.wav" [3 0]]
                         :magic    ["bfxr_curse.wav"     [6 1]])]
    (audio/play sound)
    (animation-entity/create!
     :position position
     :animation (media/fx-impact-animation fx-idx))))

(defn- blocks? [block-rate] ; TODO pass rand-number here ? ? or seeded with clojure.data.generators
  (< (rand) block-rate))

(defn- deal-damage! [{dmg-type 0 :as damage} {:keys [source target]}]
  (when-not (dead? @target)
    (cond
     (blocks? (effective-block-rate @source @target :shield dmg-type))
     (shield-blocked-effect target)
     (blocks? (effective-block-rate @source @target :armor dmg-type))
     (armor-blocked-effect target)
     :else
     (let [[dmg-type min-max-dmg] (effective-damage damage @source @target)]
       (dmg-type->hit-effect! dmg-type (:position @target))
       (effect/do! [:hp [[:val :inc] (- (random/rand-int-between min-max-dmg))]]
                   {:target target})))))

(defn- damage->text [[dmg-type [min-dmg max-dmg]]]
  (str min-dmg "-" max-dmg " " (name dmg-type) " damage"))

(effect/defeffect :damage
  {:text (fn modified-text [damage {:keys [source]}]
           (let [modified (effective-damage damage @source)]
             (if (= damage modified)
               (damage->text damage)
               (str (damage->text damage) "\nModified: " (damage->text modified)))))
   :valid-params? (fn [_ {:keys [source target]}]
                    (and source
                         target
                         (:hp @target)))
   :do! deal-damage!})
