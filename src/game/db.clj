(ns game.db
  (:require [x.x :refer [defsystem defcomponent update-map doseq-entity]]
            [game.session :as session]))

(defsystem create        [c])
(defsystem create!       [c e])
(defsystem after-create! [c e])
(defsystem destroy       [c])
(defsystem destroy!      [c e])

(def ^:private ids->entities (atom nil))

(defn get-entity [id] (get @ids->entities id))
(defn exists? [e] (get-entity (:id @e)))

(session/defstate state
  (load!  [_ data]
    (reset! ids->entities {}))
  (serialize [_])
  (initial-data [_]))

(let [cnt (atom 0)] ; TODO reset cnt every session ?
  (defn- unique-number! []
    (swap! cnt inc)))

(defcomponent :id id
  (create [_] (unique-number!)) ; TODO precondition (nil? id)
  (create!  [_ e] (swap! ids->entities assoc  id e))
  (destroy! [_ e] (swap! ids->entities dissoc id)))

(defn create-entity! [m]
  {:pre [(not (contains? m :id))]}
  (-> (assoc m :id nil)
      (update-map create)
      atom
      (doseq-entity create!)
      (doseq-entity after-create!)))

(defn destroy-to-be-removed-entities! []
  (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
          :when (exists? e)] ; TODO why is this ?
    (swap! e update-map destroy)
    (doseq-entity e destroy!)))
