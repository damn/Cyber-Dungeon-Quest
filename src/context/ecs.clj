(ns context.ecs
  (:require [clj-commons.pretty.repl :as p]
            ; TODO this only used here, good.
            ; but position-changed / moved ! is also there
            [x.x :refer [defsystem update-map doseq-entity]]
            [gdl.context :refer [draw-text]]
            [utils.core :refer [define-order sort-by-order]]
            [game.context :refer [get-entity entity-exists? get-entities-in-active-content-fields
                                  in-line-of-sight?]]))

; e = entity reference (an atom)
; e* = deref-ed entity, a map.
; c = context

; TODO always context last param
(defsystem create [_])
(defsystem create! [_ e c])

(defsystem destroy [_])
(defsystem destroy! [_ e c])

(defsystem tick  [_ delta])
(defsystem tick! [_ c e delta])

(defsystem moved! [_ e c direction-vector])

(defsystem render-below   [_ c e*])
(defsystem render-default [_ c e*])
(defsystem render-above   [_ c e*])
(defsystem render-info    [_ c e*])
(defsystem render-debug   [_ c e*])

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
         (p/pretty-pst t)
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
          system [#'render-below
                  #'render-default
                  #'render-above
                  #'render-info]
          entity* entities*]
    (render-entity* system c entity*))
  (doseq [entity* entities*]
    (render-entity* #'render-debug c entity*)))

(defn- visible-entities* [{:keys [context/player-entity] :as context}]
  (->> (get-entities-in-active-content-fields context)
       (map deref)
       (filter #(in-line-of-sight? context @player-entity %))))

(defn- tick-entity! [context entity delta]
  (swap! entity update-map tick delta)
  ; (doseq-entity entity tick! context delta)
  (doseq [k (keys @entity)] ; TODO FIXME
    (tick! [k (k @entity)] context entity delta)))


; get-entities-in-active-content-fields
; => rename just get-active-entities
; or to-be-updated-entities ?!
; dont want to know about contentfields ( ? )

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
    (doseq [entity (get-entities-in-active-content-fields context)] ; world context protocol -> get-active-entities world
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
