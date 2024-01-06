(ns cdq.context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [update-map]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [cdq.entity :as entity]
            [cdq.context :refer [transact! transact-all! get-entity create-entity! add-entity! remove-entity!]]))

(defn- render-entity* [system entity* {::keys [thrown-error] :as context}]
  (doseq [k (keys (methods @system))
          :let [v (k entity*)]
          :when v]
    (try
     (system [k v] entity* context)
     (catch Throwable t
       (when-not @thrown-error
         (println "Render error for: entity :entity/id " (:entity/id entity*) " \n k " k "\n system" system)
         (p/pretty-pst t)
         (reset! thrown-error t))
       (let [[x y] (:entity/position entity*)]
         (draw-text context {:text (str "Render error entity :entity/id " (:entity/id entity*) "\n" k "\n"system "\n" @thrown-error)
                             :x x
                             :y y
                             :up? true}))))))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defmethod cdq.context/transact! :tx/dissoc [[_ entity* k] _ctx]
  (swap! (entity/reference entity*) dissoc k)
  nil)

(defmethod cdq.context/transact! :tx/assoc [[_ entity* k v] _ctx]
  (swap! (entity/reference entity*) assoc k v)
  nil)

(defmethod cdq.context/transact! :tx/assoc-in [[_ entity* ks v] _ctx]
  (swap! (entity/reference entity*) assoc-in ks v)
  nil)

(def ^:private log-txs? true)

(defn- debug-print-tx [tx]
  (pr-str (mapv #(if (instance? cdq.entity.Entity %) (:entity/id %) %)
                tx)))

(defn- handle-transaction! [tx ctx]
  (when log-txs?
    (when-not (and (vector? tx)
                   (= :tx/cursor (first tx)))
      (println "tx: " (cond (instance? cdq.entity.Entity tx) "reset!"
                            (map? tx) "create-entity!"
                            (vector? tx) (debug-print-tx tx)))))
  (cond
   (instance? cdq.entity.Entity tx) (reset! (entity/reference tx) tx)
   (map? tx) (create-entity! ctx tx)
   (vector? tx) (doseq [tx (transact! tx ctx)]
                  (handle-transaction! tx ctx))
   :else (throw (Error. (str "Unknown transaction: " (pr-str tx))))))

(extend-type gdl.context.Context
  cdq.context/TransactionHandler
  (transact-all! [ctx txs]
    (doseq [tx txs :when tx]
      (try (handle-transaction! tx ctx)
           (catch Throwable t
             (println "Error with transaction: \n" (pr-str tx))
             (throw t))))))

(extend-type cdq.entity.Entity
  cdq.entity/HasReferenceToItself
  (reference [entity*]
    (::atom (meta entity*))))

(defn- system-transactions! [system entity ctx]
  (doseq [k (keys (methods system))
          :let [entity* @entity
                v (k entity*)]
          :when v]
    (when-let [txs (system [k v] entity* ctx)]
      (when log-txs?
        (println "txs:" (:entity/id entity*) "-" k))
      (try (transact-all! ctx txs)
           (catch Throwable t
             (println "Error with " k " and txs: \n" (pr-str txs))
             (throw t))))))

(extend-type gdl.context.Context
  cdq.context/EntityComponentSystem
  (get-entity [{::keys [ids->entities]} id]
    (get @ids->entities id))

  (create-entity! [{::keys [ids->entities] :as context} components-map]
    ; TODO all keys ':entity/'
    {:pre [(not (contains? components-map :entity/id))
           (:entity/position components-map)]}
    (try
     (let [id (unique-number!)
           entity (-> (assoc components-map :entity/id id)
                      (update-map entity/create-component)
                      cdq.entity/map->Entity
                      atom)]
       (swap! entity with-meta {::atom entity})
       (swap! ids->entities assoc id entity)
       (system-transactions! entity/create entity context)
       (add-entity! context entity) ; no left-bottom! thats why put after entity/create (x.x)
       entity)
     (catch Throwable t
       (println "Error with: " components-map)
       (throw t))))

  (tick-entity! [{::keys [thrown-error] :as context} entity]
    (try
     (system-transactions! entity/tick entity context)
     (catch Throwable t
       (p/pretty-pst t)
       (println "Entity id: " (:entity/id @entity))
       (reset! thrown-error t))))

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

  (remove-destroyed-entities! [{::keys [ids->entities] :as context}]
    (doseq [entity (filter (comp :entity/destroyed? deref) (vals @ids->entities))]
      (system-transactions! entity/destroy entity context)
      (swap! ids->entities dissoc (:entity/id @entity))
      (remove-entity! context entity))))

(defn ->context [& {:keys [z-orders]}]
  (assert (every? #(= "z-order" (namespace %)) z-orders))
  {::ids->entities (atom {})
   ::thrown-error (atom nil)
   ::render-on-map-order (define-order z-orders)})
