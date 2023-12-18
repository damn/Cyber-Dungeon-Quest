(ns game.context.ecs
  (:require [clj-commons.pretty.repl :as p]

            ; TODO this only used here, good.
            ; but position-changed / moved ! is also there
            [x.x :refer [update-map doseq-entity]]
            gdl.app
            [gdl.protocols :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [game.protocols :as gm]
            [game.entity :as entity]
            [game.line-of-sight :refer (in-line-of-sight?)]
            [game.maps.contentfields :refer [get-entities-in-active-content-fields]] ))

; TODO move x.x here also


; if lightning => pass render-on-map argument 'colorsetter' by default
; on all render-systems , has to be handled & everything has to have body then?
; so all have line of sight check & colors applied as of position ?


; TODO this is pretty printing error
; but in gdl its only on first start ?!
; no wait! it says 'app-start-failed' !! haha

(defn- render-entity* [system {:keys [context/thrown-error] :as c} entity*]
  (doseq [component entity*]
    (try
     (system component c entity*)
     (catch Throwable t
       (when-not @thrown-error
         (println "Render error for: entity :id " (:id entity*) " \n component " component "\n system" system)
         ; TODO pretty print error => same like tick => pass function through context? idk
         ; or context/handle-error ... O.O
         ; with environment ? clojure error oopts all args ? possible => can inspect then, even 'drawer' or 'context'  ?
         (reset! (:context/thrown-error c) t))
       ; TODO highlight entity ? as mouseover?
       ; TODO automatically open debug window
       (let [[x y] (:position entity*)]
         (draw-text c {:text (str "Render error entity :id " (:id entity*) "\n" (component 0) "\n"system "\n" @(:context/thrown-error c))
                       :x x
                       :y y
                       :up? true
                       }))
       ; TODO throw/catch renderfn missing & pass body ?
       ; TODO I want to get multimethod name
       ))))

(defn- render-entities* [{:keys [context/render-on-map-order] :as c} entities*]
  (doseq [[_ entities*] (sort-by-order (group-by :z-order entities*)
                                       first
                                       render-on-map-order)
          ; vars so I can see the function name @ error (can I do this with x.x? give multimethods names?)
          system [#'entity/render-below
                  #'entity/render-default
                  #'entity/render-above
                  #'entity/render-info]
          entity* entities*]
    (render-entity* system c entity*))
  (doseq [entity* entities*]
    (render-entity* #'entity/render-debug c entity*)))

(defn- visible-entities* [{:keys [context/player-entity] :as context}]
  (->> (get-entities-in-active-content-fields context)
       (map deref)
       (filter #(in-line-of-sight? @player-entity % context))))



(defn- tick-entity! [context entity delta]
  (swap! entity update-map entity/tick delta)
  ; (doseq-entity entity entity/tick! context delta)
  (doseq [k (keys @entity)] ; TODO FIXME
    (entity/tick! [k (k @entity)] context entity delta)))





; get-entities-in-active-content-fields
; => rename just get-active-entities
; or to-be-updated-entities ?!
; dont want to know about contentfields ( ? )


(extend-type gdl.protocols.Context
  game.protocols/EntityComponentSystem
  (get-entity [{:keys [context/ids->entities]} id]
    (get @ids->entities id))

  (entity-exists? [context e]
    (gm/get-entity context (:id @e)))

  (create-entity! [context components-map]
    {:pre [(not (contains? components-map :id))]}
    (-> (assoc components-map :id nil)
        (update-map entity/create)
        atom
        (doseq-entity entity/create! context)))

  (tick-active-entities [{:keys [context/thrown-error] :as context} delta]
    (try
     (doseq [entity (get-entities-in-active-content-fields context)]
       (tick-entity! context entity delta))
     (catch Throwable t
       (p/pretty-pst t)
       (reset! thrown-error t))))

  (render-visible-entities [c]
    (render-entities* c (visible-entities* c)))

  (destroy-to-be-removed-entities!
    [{:keys [context/ids->entities] :as context}]
    (doseq [e (filter (comp :destroyed? deref) (vals @ids->entities))
            :when (gm/entity-exists? context e)] ; TODO why is this ?, maybe assert ?
      (swap! e update-map entity/destroy)
      (doseq-entity e entity/destroy! context))))

; TODO use only locally?
; with higher up context/ecs key or something like that
; then I can call function on ecs, but it requires also context ...
(defn ->context [& {:keys [z-orders]}]
  {:context/ids->entities (atom {})
   :context/thrown-error (atom nil)
   :context/render-on-map-order (define-order z-orders)})

; TODO ids->entities used in :id component
; => move here
; and rename to 'ecs' just

; same maybe with cell-grids and potential field stuff ?
; == internal data structure ....
