(ns context.entity
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [defsystem doseq-entity]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [game.context :refer [get-entity]]))

; doseq-entity - what if key is not available anymore ? check :when (k @entity)  ?
; but for now accepting nil value at components, so have to check first.

(defrecord Entity [id position])

(defsystem create   [_])
(defsystem create!  [_ entity context])
(defsystem destroy! [_ entity context]) ; only used twice position/body
(defsystem tick!    [_ entity context delta])
(defsystem moved!   [_ entity context direction-vector]) ; body/position

(defsystem render-below   [_ entity* context])
(defsystem render-default [_ entity* context])
(defsystem render-above   [_ entity* context])
(defsystem render-info    [_ entity* context])
(defsystem render-debug   [_ entity* context])

(defn- render-entity* [system
                       entity*
                       {:keys [context.entity/thrown-error] :as context}]
  (doseq [component entity*]
    (try
     (system component entity* context)
     (catch Throwable t
       (when-not @thrown-error
         (println "Render error for: entity :id " (:id entity*) " \n component " component "\n system" system)
         (p/pretty-pst t)
         (reset! thrown-error t))
       (let [[x y] (:position entity*)]
         (draw-text context {:text (str "Render error entity :id " (:id entity*) "\n" (component 0) "\n"system "\n" @thrown-error)
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

(extend-type gdl.context.Context
  game.context/EntityComponentSystem
  (get-entity [{:keys [context.entity/ids->entities]} id]
    (get @ids->entities id))

  (create-entity! [{:keys [context.entity/ids->entities] :as context} components-map]
    {:pre [(not (contains? components-map :id))
           (:position components-map)]}
    (let [id (unique-number!)
          entity (-> (assoc components-map :id id)
                     (update-map create)
                     map->Entity
                     atom
                     (doseq-entity create! context))]
      (swap! ids->entities assoc id entity)
      entity))

  (tick-entity [{:keys [context.entity/thrown-error] :as context}
                entity
                delta]
    (try
     (doseq-entity entity tick! context delta)
     (catch Throwable t
       (p/pretty-pst t)
       (println "Entity id: " (:id @entity))
       (reset! thrown-error t))))

  (render-entities* [{:keys [context.entity/render-on-map-order]
                      :as context}
                     entities*]
    (doseq [entities* (map second
                           (sort-by-order (group-by :z-order entities*)
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

  (remove-destroyed-entities [{:keys [context.entity/ids->entities] :as context}]
    (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))]
      (doseq-entity e destroy! context)
      (swap! ids->entities dissoc (:id @e)))))

(defn ->context [& {:keys [z-orders]}]
  {:context.entity/ids->entities (atom {})
   :context.entity/thrown-error (atom nil)
   :context.entity/render-on-map-order (define-order z-orders)})
