(ns cdq.context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [defsystem doseq-entity]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            cdq.entity
            [cdq.context :refer [get-entity add-entity! remove-entity!]]))

; TODO
; doseq-entity - what if key is not available anymore ? check :when (k @entity)  ?
; but for now accepting nil value at components, so have to check first.

; TODO why do defsystem have default returns ?
; I can just apply them on (keys (methods ...)) only
; only 8 tick fns
; => apply on those 8 methods only of entity ....
; => reuse at effects see
; I could cache the keyset intersection call between entity keys and multimethod keys
; keySet is cached anyway in a map ?
; anyway this needs to be tested

(defsystem create   [_])
(defsystem create!  [_ entity context])
(defsystem destroy! [_ entity context])
(defsystem tick     [_ entity* context])

(defsystem render-below   [_ entity* context])
(defsystem render-default [_ entity* context])
(defsystem render-above   [_ entity* context])
(defsystem render-info    [_ entity* context])
(defsystem render-debug   [_ entity* context])

(defn- render-entity* [system
                       entity*
                       {::keys [thrown-error] :as context}]
  (doseq [component entity*]
    (try
     (system component entity* context)
     (catch Throwable t
       (when-not @thrown-error
         (println "Render error for: entity :entity/id " (:entity/id entity*) " \n component " component "\n system" system)
         (p/pretty-pst t)
         (reset! thrown-error t))
       (let [[x y] (:entity/position entity*)]
         (draw-text context {:text (str "Render error entity :entity/id " (:entity/id entity*) "\n" (component 0) "\n"system "\n" @thrown-error)
                             :x x
                             :y y
                             :up? true}))))))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

; using this because x.x/update-map with transients is destroying defrecords and
; turning them into normal maps.
; check (keys (methods multimethod))
; remove 'tick' / counters then this doesnt hurt so much anymore performance wise
(defn- update-map
  "Updates every map-entry with (apply f [k v] args)."
  [m f & args]
  (loop [ks (keys m)
         m m]
    (if (seq ks)
      (recur (rest ks)
             (let [k (first ks)]
               (assoc m k (apply f [k (k m)] args))))
      m)))


; # LATER TODO
; * create! / destroy! check too
; * manual tick ...
; * TODO check all 25 functions working

; TODO replays should be working (with different speeds)
; => memory?
; multiplayer -> only relevant transactions (movement only no full entity uudate send?

; TODO send-event / state make namespaced
; even if I have to munge/demunge it always

; TODO belongs in ecs ??
(defmulti handle-ctx-transaction! (fn [[k & _more] ctx]
                                    (assert (and (keyword? k)
                                                 (= "ctx" (namespace k))))
                                    k))

(defn- handle-side-effect! [side-effect ctx]
  (cond
   (instance? cdq.entity.Entity side-effect)
   (let [entity* side-effect]
     (reset! (::atom (meta entity*)) entity*))

   (vector? side-effect)
   (let [ctx-transaction side-effect]
     (handle-ctx-transaction! ctx-transaction ctx))))

(defn- handle-side-effects! [side-effects ctx]
  (doseq [side-effect side-effects
          :when side-effect]
    (try (handle-side-effect! side-effect ctx)
         (catch Throwable t
           (println "Error with side-effect: \n" (pr-str side-effect))
           (throw t)
           )
         )))

; TODO check the called fns are still in public context callable, can remove otherwise.

; TODO effect target/source can make '*' ?
; => using meta atom always

; TODO merge ctx with effect ctx always ? didnt do at projectile, removed the merge.
; @ active skill anwyaway merged already effect-context?
; TODO check shout is working cons ..
; [:ctx/position-changed entity*]
; [:ctx/do-effect effect-ctx effect]
; [:ctx/send-event entity :alert]

(extend-type gdl.context.Context
  cdq.context/EntityComponentSystem
  (get-entity [{::keys [ids->entities]} id]
    (get @ids->entities id))

  (create-entity! [{::keys [ids->entities] :as context} components-map]
    {:pre [(not (contains? components-map :entity/id))
           (:entity/position components-map)]}
    (try
     (let [id (unique-number!)
           entity (-> (assoc components-map :entity/id id)
                      (update-map create)
                      cdq.entity/map->Entity
                      atom
                      (doseq-entity create! context))]
       (swap! entity with-meta {::atom entity})
       (swap! ids->entities assoc id entity)
       (add-entity! context entity)
       entity)
     (catch Throwable t
       (println "Error with: " components-map)
       (throw t))))

  (tick-entity [{::keys [thrown-error] :as context} entity]
    (try
     (doseq [k (keys (methods tick))
             :let [entity* @entity
                   v (k entity*)]
             :when v]
       (let [side-effects (tick [k v] entity* context)]
         (try (handle-side-effects! side-effects context)
              (catch Throwable t
                (println "Error with " k " and side-effects: \n" (pr-str side-effects))
                ; defsystem tick default return = value ...
                ; TODO RETURN DEFAULT RETURN FOR THOSE WHO DONT NEED IT
                (throw t)))))
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
            system [#'render-below
                    #'render-default
                    #'render-above
                    #'render-info]
            entity* entities*]
      (render-entity* system entity* context))
    (doseq [entity* entities*]
      (render-entity* #'render-debug entity* context)))

  (remove-destroyed-entities [{::keys [ids->entities] :as context}]
    (doseq [entity (filter (comp :entity/destroyed? deref) (vals @ids->entities))]
      (doseq-entity entity destroy! context)
      (swap! ids->entities dissoc (:entity/id @entity))
      (remove-entity! context entity))))

(defn ->context [& {:keys [z-orders]}]
  (assert (every? #(= "z-order" (namespace %)) z-orders))
  {::ids->entities (atom {})
   ::thrown-error (atom nil)
   ::render-on-map-order (define-order z-orders)})
