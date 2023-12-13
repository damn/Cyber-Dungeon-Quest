(ns game.effects.stun
  (:require [x.x :refer [defcomponent doseq-entity]]
            [gdl.graphics.draw :as draw]
            [gdl.graphics.color :as color]
            [utils.core :refer :all]
            [game.effect :as effect]
            [game.entity :as entity]
            [game.modifier :as modifier]
            [game.utils.counter :as counter]))

(def ^:private stun-modifiers
  [[:modifiers/block :speed]
   [:modifiers/block :skillmanager]])

(defcomponent :stunned? counter
  (entity/tick [_ delta]
    (counter/tick counter delta))
  (entity/tick! [[k _] _ctx e delta]
    (when (counter/stopped? counter)
      (swap! e modifier/reverse-modifiers stun-modifiers)
      (swap! e dissoc k)))
  (entity/render-below [_ drawer _ctx {:keys [position]}]
    (draw/circle drawer position 0.5 (color/rgb 1 1 1 0.6))))

(effect/defeffect :stun
  {:text (fn [duration _]
           (str "Stuns for " (readable-number (/ duration 1000)) " seconds"))
   :valid-params? (fn [_ {:keys [source target]}]
                    (and target)) ; TODO needs :speed/:skillmanager ?!
   :do! (fn [duration {:keys [target]} _context]
          (if (:stunned? @target)
            (swap! target update-in [:stunned? :maxcnt] + duration)
            (do (doseq-entity target entity/stun!) ; TODO interrupt? (as sepearte ability also ? )
                (swap! target modifier/apply-modifiers stun-modifiers)
                (swap! target assoc :stunned? (counter/create duration)))))})
