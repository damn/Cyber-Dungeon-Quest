(nsx game.effects.stun
  (:require [game.utils.counter :as counter]))

(def ^:private stun-modifiers
  [[:block :speed]
   [:block :skillmanager]])

; TODO counter-stuff duplicated (see string effects )... sub-components create/tick/lifecycle???
; but wouldn't work with assoc??
(defcomponent :stunned? _
  (tick! [[k _] e delta]
    (when (counter/update-counter! e delta [k :counter])
      (modifiers/reverse! e stun-modifiers)
      (swap! e dissoc k)))
  (render-below [_ entity* position]
    (shape-drawer/circle position 0.5 (color/rgb 1 1 1 0.6))))

(effects/defeffect :stun
  {:text (fn [{:keys [value]}]
           (str "Stuns for " (readable-number (/ value 1000)) " seconds"))
   :valid-params? (fn [{:keys [source target]}]
                    (and target)) ; TODO needs :speed/:skillmanager ?!
   :do! (fn [{:keys [target] duration :value}]
          (if (:stunned? @target)
            (update-in! target [:stunned? :counter :maxcnt] + duration)
            (do (doseq-entity target stun!) ; TODO interrupt? (as sepearte ability also ? )
                (modifiers/apply! target stun-modifiers)
                (swap! target assoc :stunned? {:counter (counter/make-counter duration)}))))})
