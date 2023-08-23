(nsx game.components.render ; TODO 'systems/render' not components
  (:require [game.utils.counter :as counter]
            [game.ui.config :refer (hpbar-height-px)]
            game.components.delete-after-animation-stopped?))

; TODO render-order make vars so on compile time checked ?

(def render-on-map-order ; TODO could make vars with this, => no typos, compile-time check.
  (define-order
    [:on-ground ; items
     :ground    ; creatures, player
     :flying    ; flying creatures
     :effect])) ; projectiles, nova
                ; :info ( TODO only 1 -> make render-debug ? )

; TODO also add it to thrown-error ! and pause the game ....
; sometimes it only draws for 1 frame and might miss the error when not looking @ console.
#_(defn- handle-throwable [t a {:keys [id position]}]
  (let [info (str "Render error [" a " | TODO RENDERFN | id:" id "]")
        [x y] position]
    (println info)
    (println "Throwable:\n"t)
    ; red color ? (str "[RED]" ..)
    ; center-x -> alignment :center ? check opts. left/right only ?
    ; background ? -> dont have, do I need it ? maybe. with outline/border ?
    ; default font or game font ?
    ; or just highlight that entity somehow
    ; or ignore @ normal game , at debug highlight and stop game.
    (font/draw-text {:font media/font :x x,:y y,:text (str "[RED]" info)})))

; if lightning => pass render-on-map argument 'colorsetter' by default
; on all render-systems , has to be handled & everything has to have body then?
; so all have line of sight check & colors applied as of position ?
(defn- render-entity* [system m]
  (doseq [c m]
    (try
     (system c m (:position m))
     (catch Throwable t
       (println "Render error for:" [c (:id m) system])
       (throw t)
       ; TODO I want to get multimethod
       ))))
; TODO throw/catch renderfn missing & pass body ?
; TODO position needed? entity* has it in keys, we might use bottom-left

(defn render-entities* [ms] ; TODO move to game.render, only called there
  (doseq [[_ ms] (sort-by-order (group-by :z-order ms)
                                first
                                render-on-map-order)
          system [#'render-below
                  #'render
                  #'render-above
                  #'render-info]
          m ms]
    (render-entity* system m))
  (doseq [m ms]
    (render-entity* render-debug m)))

; => TODO just :text component ! !!! WTF
; TODO similar to components.clickable
(defcomponent :string-effect {:keys [text]}
  (tick! [[k _] e delta]
    (when (counter/update-counter! e delta [k :counter])
      (swap! e dissoc k)))
  (render-above [_ {:keys [body]} [x y]]
    (font/draw-text {:font media/font
                     :text text
                     :x x
                     :y (+ y (:half-height body)
                           (world/pixels->world-units hpbar-height-px))
                     :up? true})))

; TODO (destroy! => create a separate string entity which loves on for another some time
; otherwise suddenly dissappears

; TODO I dont like separate of folders into /entities or /components
; entities or components should be part of the language like 'float', 'byte' or fn
; => how can I put the things?
; map/ items/ skills/ creatures/ effects/o

; is it possible to analyze the ns structure and from there decide coupling/cohesion?

; ... what CHANGES together put together ...

; => only then possible to see patterns ^ evolve

; components & modifiers change together -> got the idea for a modiiable attribute
; thats basically what defmodifier is doing, defining (on top of  , orthogonally)
; modifiable attributes

; separate by logic 'effects' _> render at effect level, dont really do anything
; (visual,ui effects, audiovisual ???)

; move to game/entities/string-effect
; TODO pass new color/string markup stuff [COLOR]STRING\n
(defn show-string-effect [entity text]
  (if (:string-effect @entity)
    (->! entity
         (update-in [:string-effect :text] str "\n" text)
         (update-in [:string-effect :counter] counter/reset))
    (swap! entity assoc :string-effect
           {:text text
            :counter (counter/make-counter 400)})))

(defn- hp-delta-color [delta]
  (cond (pos? delta) (color/rgb 0.2 1 0.2)
        (neg? delta) (color/rgb 1 0 0)
        (zero? delta) color/white))

(defn- check-add-plus-sign [delta]
  (str (when (pos? delta) "+") delta))

(defn hp-changed-effect [entity delta]
  (show-string-effect entity
                      ;(hp-delta-color delta) ; TODO add new colors ! & TAG & with border & bigger size for damage/hp/...
                      (check-add-plus-sign delta)))

(defn mana-changed-effect [entity delta]
  ; not readable the blue color, also not interesting for enemies
  #_(show-string-effect entity
                      ; (color/rgb 0.2 0.5 1); TODO add new colors ! & TAG

                       (check-add-plus-sign delta)))

; TODO unused -> in body effect component
(defcomponent :render-filled-circle {:keys [target color]}
  (render [_ m position]
    (let [radius (+ (world/pixels->world-units 2)
                    (:radius @target))]
      (shape-drawer/filled-circle position radius color))))

; TODO unused -> in body effect component
(defn circle-around-body-render-comp
  "target entity is not the entity of this component (used at sub-entities that only share position)"
  [target color order]
  {:render-filled-circle {:target target
                          :color color}})
