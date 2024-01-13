(ns cdq.context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [defcomponent update-map apply-system]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [cdq.entity :as entity :refer [map->Entity]]
            [cdq.context :refer [transact! transact-all! get-entity]]))

(defn- apply-system-transact-all! [ctx system entity*]
  (run! #(transact-all! ctx %) (apply-system system entity* ctx)))

(defmethod transact! :tx/assoc [[_ entity k v] _ctx]
  (assert (keyword? k))
  (swap! entity assoc k v)
  nil)

(defmethod transact! :tx/assoc-in [[_ entity ks v] _ctx]
  (swap! entity assoc-in ks v)
  nil)

(defmethod transact! :tx/dissoc [[_ entity k] _ctx]
  (assert (keyword? k))
  (swap! entity dissoc k)
  nil)

(defmethod transact! :tx/dissoc-in [[_ entity ks] _ctx]
  (assert (> (count ks) 1))
  (swap! entity update-in (drop-last ks) dissoc (last ks))
  nil)

; simple rule:
; all side effects have to be in a tx which returns nil and lead to same side effects

(def id-counter (atom 0))

(defn- unique-number! []
  (swap! id-counter inc))

; sets the specific atom to initial-value ( pass uid also)
; this atom is used in all future txs of that entity ! its the entity/id !
(defmethod transact! :tx/setup-entity [[_ an-atom components] ctx]
  {:pre [(not (contains? components :entity/id))
         (not (contains? components :entity/uid))]}
  (let [entity* (-> components
                    (update-map entity/create-component components ctx)
                    map->Entity)]
    (reset! an-atom (assoc entity*
                           :entity/id an-atom
                           :entity/uid (unique-number!))))  ; TODO maybe pass in tx-data then do not need to reset id-counter
  nil)

(defmethod transact! :tx/assoc-uids->entities [[_ entity] {::keys [uids->entities]}]
  {:pre [(number? (:entity/uid @entity))]}
  (swap! uids->entities assoc (:entity/uid @entity) entity)
  nil)

(defmethod transact! :tx/dissoc-uids->entities [[_ uid] {::keys [uids->entities]}]
  {:pre [(contains? @uids->entities uid)]}
  (swap! uids->entities dissoc uid)
  nil)

(defcomponent :entity/uid uid
  (entity/create  [_ {:keys [entity/id]} _ctx] [[:tx/assoc-uids->entities   id]])
  (entity/destroy [_ _entity*            _ctx] [[:tx/dissoc-uids->entities uid]]))

(defmethod transact! :tx/create [[_ components] ctx]
  (let [entity (atom nil)]
    (transact-all! ctx [[:tx/setup-entity entity components]])
    (apply-system-transact-all! ctx entity/create @entity))
  [])

(defmethod transact! :tx/destroy [[_ entity] _ctx]
  (swap! entity assoc :entity/destroyed? true)
  nil)

(defn- render-entity* [system entity* {::keys [thrown-error] :as ctx}]
  (try
   (dorun (apply-system system entity* ctx))
   (catch Throwable t
     (when-not @thrown-error
       (p/pretty-pst t)
       (reset! thrown-error t))
     (let [[x y] (:entity/position entity*)]
       (draw-text ctx {:text (str "Error " (:entity/uid entity*))
                       :x x
                       :y y
                       :up? true})))))

(def ^:private render-systems [entity/render-below
                               entity/render-default
                               entity/render-above
                               entity/render-info])

(extend-type gdl.context.Context
  cdq.context/EntityComponentSystem
  (get-entity [{::keys [uids->entities]} uid]
    (get @uids->entities uid))

  (tick-entities! [{::keys [thrown-error] :as ctx} entities*]
    (doseq [entity* entities*]
      (try
       (apply-system-transact-all! ctx entity/tick entity*)
       (catch Throwable t
         (p/pretty-pst t)
         (reset! thrown-error t)))))

  (render-entities! [{::keys [render-on-map-order] :as context} entities*]
    (doseq [entities* (map second
                           (sort-by-order (group-by :entity/z-order entities*)
                                          first
                                          render-on-map-order))
            system render-systems
            entity* entities*]
      (render-entity* system entity* context))
    (doseq [entity* entities*]
      (render-entity* entity/render-debug entity* context)))

  (remove-destroyed-entities! [{::keys [uids->entities] :as ctx}]
    (doseq [entity (filter (comp :entity/destroyed? deref) (vals @uids->entities))]
      (apply-system-transact-all! ctx entity/destroy @entity))))

(defn ->context [& {:keys [z-orders]}]
  (assert (every? #(= "z-order" (namespace %)) z-orders))
  {::uids->entities (atom {})
   ::thrown-error (atom nil)
   ::render-on-map-order (define-order z-orders)})
