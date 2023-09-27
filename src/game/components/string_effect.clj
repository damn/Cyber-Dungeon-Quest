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
