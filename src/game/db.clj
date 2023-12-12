(ns game.db
  (:require [x.x :refer [defcomponent update-map doseq-entity]]
            [game.entity :as entity]
            [game.session :as session]))

(def ^:private ids->entities (atom nil))

(defn get-entity [id] (get @ids->entities id))
(defn exists? [e] (get-entity (:id @e)))

(def state (reify session/State
             (load!  [_ data]
               (reset! ids->entities {}))
             (serialize [_])
             (initial-data [_])))

(let [cnt (atom 0)] ; TODO reset cnt every session ?
  (defn- unique-number! []
    (swap! cnt inc)))

(defcomponent :id id
  (entity/create [_] (unique-number!)) ; TODO precondition (nil? id)
  (entity/create!  [_ e _ctx]
    (swap! ids->entities assoc id e))
  (entity/destroy! [_ e _ctx]
    (swap! ids->entities dissoc id)))

(defn create-entity! [m context]
  {:pre [(not (contains? m :id))]}
  (-> (assoc m :id nil)
      (update-map entity/create)
      atom
      (doseq-entity entity/create! context)))

(defn destroy-to-be-removed-entities! [context]
  (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
          :when (exists? e)] ; TODO why is this ?
    (swap! e update-map entity/destroy)
    (doseq-entity e entity/destroy! context)))
