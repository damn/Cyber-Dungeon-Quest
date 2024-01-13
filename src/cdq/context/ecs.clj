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

; I could also pass to setup-entity the uid, then the counter will not be used and does not need to match up
(def id-counter (atom 0))

(defn- unique-number! []
  (swap! id-counter inc))

; TODO its really complicated, re-using atoms, having an uid counter which has to match up etc.
; setting up entity ...

; => because atom has to match up, all txs are with the atom
; or I convert each txs to uid and have to chance transact! fns

; giving , saving the initial value as tx
; and setting with newid always (doesnt matter reuse ids , not using other than debugging)
; by the way could add debug helper rightclick adds the entity in src/dev.clj to a var ?

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
                           :entity/uid (unique-number!)))) ; need to make here so the component systems are called, otherwise it does not have those components
  nil)

;; START uid system - only for debugging entities - orthogonal to rest of code - NOT! because the uid component needs to be added

; TODO why pass both? entity contains the uid ? this was the problem before !
; stupid uid !!
; see that each code is simpel
(defmethod transact! :tx/assoc-uids->entities [[_ entity uid] {::keys [uids->entities]}]
  {:pre [(:entity/uid @uids->entities uid)]}
  (swap! uids->entities assoc uid entity)
  nil)

(defmethod transact! :tx/dissoc-uids->entities [[_ uid] {::keys [uids->entities]}]
  {:pre [(contains? @uids->entities uid)]}
  (println "(swap! uids->entities dissoc uid) " uid)
  (println "before: (contains? @uids->entities uid)" (contains? @uids->entities uid))
  (swap! uids->entities dissoc uid)
  (println "after: (contains? @uids->entities uid)" (contains? @uids->entities uid))
  nil)

(defcomponent :entity/uid uid
  (entity/create  [_ {:keys [entity/id]}  _ctx] [[:tx/assoc-uids->entities id uid]])
  (entity/destroy [_ _entity*             _ctx] [[:tx/dissoc-uids->entities uid]]))

;; END uid system - only for debugging entities - orthogonal to rest of code


; TODO don't call transact-all! in a transact!, just return the txs ??
; but for effect need extra ctx??

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

    (let [cnt #(count (filter (comp :entity/destroyed? deref) (vals @uids->entities)))]
      (when (>  (cnt) 0)
        (println "~~ remove-destroyed-entities! count: " (cnt)))

      (doseq [entity (filter (comp :entity/destroyed? deref) (vals @uids->entities))]

        (println "remove-destroyed-entities! on " @entity)

        (apply-system-transact-all! ctx entity/destroy @entity))

      (when (> (cnt) 0)
        (let [entity (first (filter (comp :entity/destroyed? deref) (vals @uids->entities)))]
          (println " ~~~ after removed doseq => count is " (cnt))
          (println "Remaining entity: " (select-keys @entity [:entity/uid]))
          (println "after all doseq,  (contains? @uids->entities (:entity/uid @entity))" (contains? @uids->entities (:entity/uid @entity))))
        )

      ; => count was '1' ! then one I destroyed by attacking !
      )))

(defn ->context [& {:keys [z-orders]}]
  (assert (every? #(= "z-order" (namespace %)) z-orders))
  {::uids->entities (atom {})
   ::thrown-error (atom nil)
   ::render-on-map-order (define-order z-orders)})
