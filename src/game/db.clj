(ns game.db
  (:require [x.x :refer [update-map doseq-entity]]
            [game.entity :as entity]))

(defn get-entity [{:keys [context/ids->entities]} id]
  (get @ids->entities id))

(defn exists? [context e]
  (get-entity context (:id @e)))

(defn create-entity! [context components-map]
  {:pre [(not (contains? components-map :id))]}
  (-> (assoc components-map :id nil)
      (update-map entity/create)
      atom
      (doseq-entity entity/create! context)))

(defn destroy-to-be-removed-entities!
  [{:keys [context/ids->entities] :as context}]
  (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
          :when (exists? e)] ; TODO why is this ?
    (swap! e update-map entity/destroy)
    (doseq-entity e entity/destroy! context)))
