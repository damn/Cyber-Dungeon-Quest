(ns game.effects.stun
  (:require [x.x :refer [defsystem defcomponent doseq-entity]]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.color :as color]
            [utils.core :refer :all]
            [game.tick :refer [tick!]]
            [game.render :as render]
            [game.effect :as effect]
            [game.modifier :as modifier]
            [game.utils.counter :as counter]))

(def ^:private stun-modifiers
  [[:modifiers/block :speed]
   [:modifiers/block :skillmanager]])

; TODO counter-stuff duplicated (see string effects )... sub-components create/tick/lifecycle???
; but wouldn't work with assoc??
(defcomponent :stunned? _
  (tick! [[k _] e delta]
    (when (counter/update-counter! e delta [k :counter])
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
            (update-in! target [:stunned? :counter :maxcnt] + duration)
            (do (doseq-entity target stun!) ; TODO interrupt? (as sepearte ability also ? )
                (swap! target modifier/apply-modifiers stun-modifiers)
                (swap! target assoc :stunned? {:counter (counter/make-counter duration)}))))})
