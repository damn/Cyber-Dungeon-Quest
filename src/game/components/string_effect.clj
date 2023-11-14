(ns game.components.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.font :as font]
            [gdl.graphics.world :as world]
            [game.utils.counter :as counter]
            [game.ui.config :refer [hpbar-height-px]]
            [game.media :as media]
            [game.tick :refer [tick!]]
            [game.render :as render]))

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

(defn add [entity* text]
  (if (:string-effect entity*)
    (update entity* :string-effect #(-> %
                                        (update :text str "\n" text)
                                        (update :counter counter/reset)))
    (assoc entity* :string-effect {:text text
                                   :counter (counter/make-counter 400)})))
