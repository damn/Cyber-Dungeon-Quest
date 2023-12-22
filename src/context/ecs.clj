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
                       {:keys [context/thrown-error] :as context}]
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

(extend-type gdl.context.Context
  game.context/EntityComponentSystem
  (get-entity [{:keys [context/ids->entities]} id]
    (get @ids->entities id))

  (create-entity! [context components-map]
    {:pre [(not (contains? components-map :id))]}
    (-> (assoc components-map :id nil)
        (update-map create)
        atom
        (doseq-entity create! context)))

  (tick-entity [{:keys [context/thrown-error] :as context}
                entity
                delta]
    (try
     (swap! entity update-map tick delta)
     (doseq-entity entity tick! context delta)
     (catch Throwable t
       (p/pretty-pst t)
       (println "Entity id: " (:id @entity))
       (reset! thrown-error t))))

  (render-entities* [{:keys [context/render-on-map-order]
                                       :as context}
                                      entities*]
    (doseq [[_ entities*] (sort-by-order (group-by :z-order entities*)
                                         first
                                         render-on-map-order)
            ; vars so I can see the function name @ error (can I do this with x.x? give multimethods names?)
            system [#'render-below
                    #'render-default
                    #'render-above
                    #'render-info]
            entity* entities*]
      (render-entity* system entity* context))
    (doseq [entity* entities*]
      (render-entity* #'render-debug entity* context)))

  (remove-destroyed-entities [{:keys [context/ids->entities] :as context}]
    (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))]
      (swap! e update-map destroy)
      (doseq-entity e destroy! context))))

; TODO check 'internal' data structure use anywhere (id comp..)
; maybe namespaced keyword pattern '::' ?
(defn ->context [& {:keys [z-orders]}]
  {:context/ids->entities (atom {})
   :context/thrown-error (atom nil)
   :context/render-on-map-order (define-order z-orders)})
