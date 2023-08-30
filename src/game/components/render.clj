(ns game.components.render ; TODO 'systems/render' not components
  (:require [gdl.graphics.font :as font]
            [utils.core :refer [define-order sort-by-order]]
            [game.systems :refer [render-below render render-above render-debug render-info]]))

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
