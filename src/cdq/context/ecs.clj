(ns cdq.context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [defcomponent update-map]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [cdq.entity :as entity :refer [map->Entity]]
            [cdq.context :refer [transact! transact-all! get-entity add-entity! remove-entity!]]))

(def ^:private log-systems? false)

(defn- apply-system [system-var entity* ctx]
  (for [k (keys (methods @system-var))
        :let [v (k entity*)]
        :when v]
    (do
     (when log-systems? (println [(:entity/uid entity*) k]))
     (try (system-var [k v] entity* ctx)
          (catch Throwable t
            (println "Error with entity" (:entity/uid entity*) "defcomponent:" k "system:" system-var)
            (throw t))))))

(defn- apply-system-transact-all! [ctx system-var entity*]
  (run! #(transact-all! ctx %) (apply-system system-var entity* ctx)))

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

(defcomponent :entity/uid uid
  (entity/create [_ {:keys [entity/id]} {::keys [uids->entities]}]
    (swap! uids->entities assoc uid id)
    nil)

  (entity/destroy [_ _entity* {::keys [uids->entities]}]
    (swap! uids->entities dissoc uid)
    nil))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defmethod transact! :tx/create [[_ components] ctx]
  {:pre [(not (contains? components :entity/id))
         (not (contains? components :entity/uid))]}
  (let [entity (-> components
                   (assoc :entity/uid (unique-number!))
                   (update-map entity/create-component ctx)
                   map->Entity
                   atom)]
    (swap! entity assoc :entity/id entity)
    (apply-system-transact-all! ctx #'entity/create @entity)
    (add-entity! ctx entity))
  nil)

(defmethod transact! :tx/destroy [[_ entity] _ctx]
  (swap! entity assoc :entity/destroyed? true)
  nil)

(defn- render-entity* [system-var entity* {::keys [thrown-error] :as ctx}]
  (try
   (dorun (apply-system system-var entity* ctx))
   (catch Throwable t
     (when-not @thrown-error
       (p/pretty-pst t)
       (reset! thrown-error t))
     (let [[x y] (:entity/position entity*)]
       (draw-text ctx {:text (str "Render error entity " (:entity/uid entity*)
                                  "\n"system-var
                                  "\n" @thrown-error)
                       :x x
                       :y y
                       :up? true})))))

(extend-type cdq.entity.Entity
  entity/Tick
  (tick! [entity* {::keys [thrown-error] :as ctx}]
    (try
     (apply-system-transact-all! ctx #'entity/tick entity*)
     (catch Throwable t
       (p/pretty-pst t)
       (reset! thrown-error t)))))

(extend-type gdl.context.Context
  cdq.context/EntityComponentSystem
  ; uid->entity ?
  (get-entity [{::keys [uids->entities]} uid]
    (get @uids->entities uid))

  (render-entities* [{::keys [render-on-map-order] :as context} entities*]
    (doseq [entities* (map second
                           (sort-by-order (group-by :entity/z-order entities*)
                                          first
                                          render-on-map-order))
            ; vars so I can see the function name @ error (can I do this with x.x? give multimethods names?)
            system [#'entity/render-below
                    #'entity/render-default
                    #'entity/render-above
                    #'entity/render-info]
            entity* entities*]
      (render-entity* system entity* context))
    (doseq [entity* entities*]
      (render-entity* #'entity/render-debug entity* context)))

  (remove-destroyed-entities! [{::keys [uids->entities] :as ctx}]
    (doseq [entity (filter (comp :entity/destroyed? deref) (vals @uids->entities))]
      (apply-system-transact-all! ctx #'entity/destroy @entity)
      (remove-entity! ctx entity))))

(defn ->context [& {:keys [z-orders]}]
  (assert (every? #(= "z-order" (namespace %)) z-orders))
  {::uids->entities (atom {})
   ::thrown-error (atom nil)
   ::render-on-map-order (define-order z-orders)})
