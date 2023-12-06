(ns game.components.string-effect
  (:require [x.x :refer [defcomponent]]
            [gdl.graphics.font :as font]
            [gdl.graphics.world :as world]
            [game.utils.counter :as counter]
            [game.ui.config :refer [hpbar-height-px]]
            [game.entity :as entity]))

(defcomponent :string-effect {:keys [text counter] :as this}
  (entity/tick [_ delta]
    (update this :counter counter/tick delta))
  (entity/tick! [[k _] e delta]
    (when (counter/stopped? counter)
      (swap! e dissoc k)))
  (entity/render-above [_ context {[x y] :position :keys [body]}]
    (font/draw-text context
                    {:text text
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
                                   :counter (counter/create 400)})))
