(ns context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [x.x :refer [defsystem update-map doseq-entity]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [game.context :refer [get-entity entity-exists? get-active-entities line-of-sight?]]))

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

(defn- render-entities* [{:keys [context/render-on-map-order]
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

; TODO getting 3 times active entities: render, tick, potential-field =>
; just 1 app/game render fn and calculate once ? & delta in context ?
(defn- visible-entities* [{:keys [context/player-entity] :as context}]
  (->> (get-active-entities context)
       (map deref)
       (filter #(line-of-sight? context @player-entity %))))

(defn- tick-entity! [context entity delta]
  (swap! entity update-map tick delta)
  (doseq-entity entity tick! context delta))

(extend-type gdl.context.Context
  game.context/EntityComponentSystem
  (get-entity [{:keys [context/ids->entities]} id]
    (get @ids->entities id))

  (entity-exists? [context e]
    (get-entity context (:id @e)))

  (create-entity! [context components-map]
    {:pre [(not (contains? components-map :id))]}
    (-> (assoc components-map :id nil)
        (update-map create)
        atom
        (doseq-entity create! context)))

  (tick-active-entities
    [{:keys [context/thrown-error] :as context} delta]
    (doseq [entity (get-active-entities context)]
      (try
       (tick-entity! context entity delta)
       (catch Throwable t
         (p/pretty-pst t)
         (println "Entity id: " (:id @entity))
         (reset! thrown-error t)))))

  (render-visible-entities [c]
    (render-entities* c (visible-entities* c)))

  (destroy-to-be-removed-entities!
    [{:keys [context/ids->entities] :as context}]
    (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
            :when (entity-exists? context e)] ; TODO why is this ?, maybe assert ?
      (swap! e update-map destroy)
      (doseq-entity e destroy! context))))

; TODO use only locally?
; with higher up context/ecs key or something like that
; then I can call function on ecs, but it requires also context ...
(defn ->context [& {:keys [z-orders]}]
  {:context/ids->entities (atom {})
   :context/thrown-error (atom nil) ; naming ? context/ecs ? so know error is from ecs ?
   :context/render-on-map-order (define-order z-orders)})

; TODO ids->entities used in :id component
; => move here
; and rename to 'ecs' just

; same maybe with and potential field stuff ?
; == internal data structure ....
