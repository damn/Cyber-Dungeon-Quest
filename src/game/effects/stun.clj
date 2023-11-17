(ns game.effects.stun
  (:require [x.x :refer [defsystem defcomponent doseq-entity]]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.color :as color]
            [utils.core :refer :all]
            [game.tick :refer [tick tick!]]
            [game.render :as render]
            [game.effect :as effect]
            [game.modifier :as modifier]
            [game.utils.counter :as counter]))

(def ^:private stun-modifiers
  [[:modifiers/block :speed]
   [:modifiers/block :skillmanager]])

(defcomponent :stunned? counter
  (tick [_ delta]
    (counter/tick counter delta))
  (tick! [[k _] e delta]
    (when (counter/stopped? counter)
      (swap! e modifier/reverse-modifiers stun-modifiers)
      (swap! e dissoc k)))
  (render/below [_ entity* position]
    (shape-drawer/circle position 0.5 (color/rgb 1 1 1 0.6))))

(defsystem stun! [c e])

(effect/defeffect :stun
  {:text (fn [duration _]
           (str "Stuns for " (readable-number (/ duration 1000)) " seconds"))
   :valid-params? (fn [_ {:keys [source target]}]
                    (and target)) ; TODO needs :speed/:skillmanager ?!
   :do! (fn [duration {:keys [target]}]
          (if (:stunned? @target)
            (update-in! target [:stunned? :maxcnt] + duration)
            (do (doseq-entity target stun!) ; TODO interrupt? (as sepearte ability also ? )
                (swap! target modifier/apply-modifiers stun-modifiers)
                (swap! target assoc :stunned? (counter/create duration)))))})
