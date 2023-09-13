(ns game.effects.damage ; TODO definitely needs tests !! start with val-max ..
  (:require [gdl.audio :as audio]
            [data.val-max :refer [apply-val-max-modifiers]]
            [game.media :as media]
            [game.effect :as effect]
            [game.utils.random :as random]
            [game.components.string-effect :refer [show-string-effect]]
            [game.entities.animation :as animation-entity]
            [game.components.hp :refer (dead?)]))

(defn- effect-modifiers [{:keys [source target]} effect-type]
  {:source-modifiers (-> @source :effect-modifiers effect-type :source)
   :target-modifiers (when target
                       (-> @target :effect-modifiers effect-type :target))})

(defn- blocks? [shield-or-armor {:keys [source target value] :as params}]
  (let [[damage-type damage] value
        {:keys [source-modifiers
                target-modifiers]} (effect-modifiers params shield-or-armor)
        source-block-ignore (damage-type source-modifiers)
        target-block        (damage-type target-modifiers)
        block-rate (max (- (or target-block 0)
                           (or source-block-ignore 0))
                        0)]
    (< (rand) block-rate)))

(defn- apply-damage-modifiers [{:keys [source target value] :as params}]
  (let [[damage-type damage] value
        {:keys [source-modifiers
                target-modifiers]} (effect-modifiers params :damage)
        source-modifiers (damage-type source-modifiers)
        target-modifiers (damage-type target-modifiers)
        damage (if source-modifiers
                 (apply-val-max-modifiers damage source-modifiers)
                 damage)
        damage (if target-modifiers
                 (apply-val-max-modifiers damage target-modifiers)
                 damage)]
    [damage-type damage]))

(comment
 ; TODO is outdated, redo.
 (apply-modifiers
  {:source-modifiers {:physical  {[:max :mult] 2
                                  [:val :mult] 1.5
                                  [:val :inc] 1
                                  [:max :inc] 0
                                  :armor-ignore 0
                                  :shield-ignore 0}}
   :target-modifiers {:physical  {[:max :mult] 1
                                  [:val :mult] 1
                                  [:val :inc] -5
                                  [:max :inc] 0
                                  :armor-block 0.3
                                  :shield-block 0.5}}
   :value [:physical [3 10]]})
 ; =>
 {:damage [6 20]} ; or shield or armor
 )

(defn- shield-blocked-effect [entity]
  (show-string-effect entity "SHIELD"))

(defn- armor-blocked-effect [entity]
  ; (audio/play "bfxr_armorhit.wav")
  (show-string-effect entity "ARMOR"))

(defn- damage-type->hit-effect! [damage-type position]
  (let [[sound fx-idx] (case damage-type
                         :physical ["bfxr_normalhit.wav" [3 0]]
                         :magic    ["bfxr_curse.wav"     [6 1]])]
    (audio/play sound)
    (animation-entity/create!
     :position position
     :animation (media/fx-impact-animation fx-idx))))

(defn- deal-damage! [{:keys [target] [damage-type] :value :as params}]
  (when-not (dead? @target)
    (cond
     (blocks? :shield params) (shield-blocked-effect target)
     (blocks? :armor  params) (armor-blocked-effect  target)
     :else
     (let [[damage-type damage] (apply-damage-modifiers params)
           damage (random/rand-int-between damage)]
       (damage-type->hit-effect! damage-type
                                 (:position @target))
       (effect/do-effect! {:target target}
                           [:hp [[:val :inc] (- damage)]])))))

#_(defn- get-dps [[mi mx] seconds]
  (round-n-decimals (/ (+ mi mx) 2 seconds) 2))

#_(defn- get-sword-infostr [{:keys [base-dmg] :as skill}]
  (let [seconds 0.3]
    (str
      (variance-val-str base-dmg) " Damage\n"
      "Normal " " Attack-Speed\n"
      (str "DPS: " (get-dps base-dmg seconds)))))

(defn- damage-text [[damage-type damage-amount]]
  (str (damage-amount 0) "-" (damage-amount 1) " " (name damage-type) " damage"))

(defn- damage-infotext
  [{:keys [value] :as params}] ; value = [:physical [5 6]] for example
  (let [modified (apply-damage-modifiers params)]
    (if (= value modified)
      (damage-text value)
      (str
       (damage-text value)
       "\n"
       "Modified: " (damage-text (apply-damage-modifiers params))))))

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
(effect/defeffect :damage
  {:text damage-infotext
   :valid-params? (fn [{:keys [source target]}]
                    (and source
                         target
                         (:hp @target)))
   :do! deal-damage!})
