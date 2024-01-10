(ns cdq.context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [update-map]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [cdq.entity :as entity :refer [map->Entity]]
            [cdq.context :refer [transact! transact-all! get-entity add-entity! remove-entity!]]))

(defn- render-entity* [system entity* {::keys [thrown-error] :as context}]
  (doseq [k (keys (methods @system))
          :let [v (k entity*)]
          :when v]
    (try
     (system [k v] entity* context)
     (catch Throwable t
       (when-not @thrown-error
         (println "Render error for: entity " (:entity/uid entity*) " \n k " k "\n system" system)
         (p/pretty-pst t)
         (reset! thrown-error t))
       (let [[x y] (:entity/position entity*)]
         (draw-text context {:text (str "Render error entity " (:entity/uid entity*) "\n" k "\n"system "\n" @thrown-error)
                             :x x
                             :y y
                             :up? true}))))))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

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

(declare create-entity!)

(defmethod transact! :tx/create [[_ components] ctx]
  (create-entity! ctx components)
  nil)

(defmethod transact! :tx/destroy [[_ entity] _ctx]
  (swap! entity assoc :entity/destroyed? true)
  nil)

(def ^:private log-txs? false)

(defn- debug-print-tx [tx]
  (pr-str (mapv #(cond
                  (instance? clojure.lang.Atom %) (:entity/uid @%)
                  (instance? gdl.backends.libgdx.context.image_drawer_creator.Image %) "<Image>"
                  (instance? gdl.graphics.animation.ImmutableAnimation %) "<Animation>"
                  (instance? gdl.context.Context %) "<Context>"
                  :else %)
                tx)))

(defn- log-tx [tx]
  (when-not (= :tx/cursor (first tx))
    (println (debug-print-tx tx))))

(extend-type gdl.context.Context
  cdq.context/TransactionHandler
  (transact-all! [ctx txs]
    (doseq [tx txs :when tx]
      (when log-txs? (log-tx tx))
      (try (transact-all! ctx (transact! tx ctx))
           (catch Throwable t
             (println "Error with transaction: \n" (debug-print-tx tx))
             (throw t))))))

(defn- system-transactions! [system entity ctx]
  (doseq [k (keys (methods system))
          :let [entity* @entity
                v (k entity*)]
          :when v]
    (let [txs (system [k v] entity* ctx)]
      (when (seq txs)
        (when log-txs?
          (println (:entity/uid entity*) "-" k))
        (try (transact-all! ctx txs)
             (catch Throwable t
               (println "Error with " k " and txs: \n" (map debug-print-tx txs))
               (throw t)))))))

(defn- create-entity! [{::keys [uids->entities] :as context} components-map]
  {:pre [(not (contains? components-map :entity/id))
         (not (contains? components-map :entity/uid))
         (:entity/position components-map)]}
  (try
   (let [entity (-> components-map
                    (update-map entity/create-component context)
                    map->Entity
                    atom)
         uid (unique-number!)]
     (swap! entity assoc :entity/id entity :entity/uid uid)
     (swap! uids->entities assoc uid entity)
     (system-transactions! entity/create entity context)
     (add-entity! context entity)
     entity)
   (catch Throwable t
     (println "Error with: " components-map)
     (throw t))))

(extend-type gdl.context.Context
  cdq.context/EntityComponentSystem
  (get-entity [{::keys [uids->entities]} id]
    (get @uids->entities id))

  (tick-entity! [{::keys [thrown-error] :as context} entity]
    (try
     (system-transactions! entity/tick entity context)
     (catch Throwable t
       (p/pretty-pst t)
       (println "Entity id: " (:entity/uid @entity))
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

  (remove-destroyed-entities! [{::keys [uids->entities] :as context}]
    (doseq [entity (filter (comp :entity/destroyed? deref) (vals @uids->entities))]
      (system-transactions! entity/destroy entity context)
      (swap! uids->entities dissoc (:entity/uid @entity))
      (remove-entity! context entity))))

(defn ->context [& {:keys [z-orders]}]
  (assert (every? #(= "z-order" (namespace %)) z-orders))
  {::uids->entities (atom {})
   ::thrown-error (atom nil)
   ::render-on-map-order (define-order z-orders)})
