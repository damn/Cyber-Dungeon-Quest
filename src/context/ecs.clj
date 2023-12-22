(ns context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [defsystem update-map doseq-entity]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [game.context :refer [get-entity]]))

(defsystem create [_])
(defsystem create! [_ entity context])

(defsystem destroy [_])
(defsystem destroy! [_ entity context])

(defsystem tick  [_ delta])
(defsystem tick! [_ entity context delta])

(defsystem moved! [_ entity context direction-vector])

(defsystem render-below   [_ entity* context])
(defsystem render-default [_ entity* context])
(defsystem render-above   [_ entity* context])
(defsystem render-info    [_ entity* context])
(defsystem render-debug   [_ entity* context])

(defn- render-entity* [system
                       entity*
                       {:keys [context.ecs/thrown-error] :as context}]
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

(extend-type gdl.context.Context
  game.context/EntityComponentSystem
  (get-entity [{:keys [context.ecs/ids->entities]} id]
    (get @ids->entities id))

  (create-entity! [{:keys [context.ecs/ids->entities]} components-map]
    {:pre [(not (contains? components-map :id))]}
    (let [entity (-> (assoc components-map :id (unique-number!))
                     (update-map create)
                     atom
                     (doseq-entity create! context))]
      (swap! ids->entities assoc id entity)
      entity))

  (tick-entity [{:keys [context.ecs/thrown-error] :as context}
                entity
                delta]
    (try
     (swap! entity update-map tick delta)
     (doseq-entity entity tick! context delta)
     (catch Throwable t
       (p/pretty-pst t)
       (println "Entity id: " (:id @entity))
       (reset! thrown-error t))))

  (render-entities* [{:keys [context.ecs/render-on-map-order]
                      :as context}
                     entities*]
    (doseq [entities* (second (sort-by-order (group-by :z-order entities*)
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

  (remove-destroyed-entities [{:keys [context.ecs/ids->entities] :as context}]
    (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))]
      (swap! e update-map destroy)
      (doseq-entity e destroy! context)
      (swap! ids->entities dissoc (:id @e)))))

(defn ->context [& {:keys [z-orders]}]
  {:context.ecs/ids->entities (atom {})
   :context.ecs/thrown-error (atom nil)
   :context.ecs/render-on-map-order (define-order z-orders)})
