(ns game.db
  (:require [x.x :refer :all]
            [x.session :as session]))

(defsystem create        [c])
(defsystem create!       [c e])
(defsystem after-create! [c e])
(defsystem destroy       [c])
(defsystem destroy!      [c e])

(def ^:private ids->entities (atom nil))

(defn get-entity [id] (get @ids->entities id))
(defn exists? [e] (get-entity (:id @e)))

; TODO state also a system for a db component !
; db is component of world, like cell-grid, etc. ??
; they also have create/destroy (no destroy ? )
; and create-from / and load from ?
; world is component of systems ! where we call manually tick in right oder!
; defstate !
; and serialize/initial/data is all optional !
; x.system.db is a component of the whole game-entity.
(session/defstate db-state ; TODO we are :refer :all this everywhere !
  (load!  [_ data]
    (reset! ids->entities {}))
  (serialize [_])
  (initial-data [_]))

(comment
 (defcomponent :db
   (load! [_ init-data] (reset! ids->entities init-data))
   (serialize [[_ v]] v)
   (initial-data [_] {})))

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

; TODO This is a system which is done every tick (manually)
; destroy system !
(defn destroy-to-be-removed-entities! []
  (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
          :when (exists? e)] ; TODO why is this ?
    (swap! e update-map destroy)
    (doseq-entity e destroy!)))
