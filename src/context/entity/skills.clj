(ns context.entity.skills
  (:require [x.x :refer [defcomponent]]
            [data.counter :as counter]
            [utils.core :refer [mapvals]]
            [context.entity :as entity]
            [game.context :refer [get-property valid-params?]]))

(defn- update-cooldown [skill delta]
  (if (:cooling-down? skill)
    (update skill :cooling-down?
            #(let [counter (counter/tick % delta)]
               (if (counter/stopped? counter)
                 false
                 counter)))
    skill))

(defcomponent :skills skills
  (entity/create! [_ entity context]
    (swap! entity update :skills
           #(zipmap %
                    (map (fn [skill-id] (get-property context skill-id))
                         %))))
  (entity/tick [_ delta]
    (mapvals #(update-cooldown % delta) skills)))

(defn has-skill? [skills {:keys [id]}]
  (contains? skills id))

(defn add-skill [skills {:keys [id] :as skill}]
  (assert (not (has-skill? skills skill)))
  (assoc skills id skill))

(defn remove-skill [skills {:keys [id] :as skill}]
  (assert (has-skill? skills skill))
  (dissoc skills id))

(defn set-skill-to-cooldown [skills {:keys [id cooldown] :as skill}]
  (if cooldown
    (assoc-in skills [id :cooling-down?] (counter/create cooldown))
    skills))

(defn usable-state [effect-context
                    {:keys [mana]}
                    {:keys [cost cooling-down? effect]}]
  (cond
   cooling-down?
   :cooldown

   (and cost (> cost (mana 0)))
   :not-enough-mana

   (not (valid-params? effect-context effect))
   :invalid-params

   :else
   :usable))
