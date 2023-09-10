(ns game.components.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.font :as font]
            [gdl.graphics.world :as world]
            [gdl.graphics.color :as color]
            [utils.core :refer [->!]]
            [game.utils.counter :as counter]
            [game.ui.config :refer [hpbar-height-px]]
            [game.media :as media]
            [game.tick :refer [tick!]]
            [game.render :as render]))

; TODO similar to components.clickable
(defcomponent :string-effect {:keys [text]}
  (tick! [[k _] e delta]
    (when (counter/update-counter! e delta [k :counter])
      (swap! e dissoc k)))
  (render/above [_ {:keys [body]} [x y]]
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
; map/ items/ skills/ creatures/ effect/o

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
