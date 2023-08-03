(nsx game.effects.stun)

(def ^:private stun-modifiers
  [[:block :speed]
   [:block :skillmanager]])

(defcomponent ::stun-effect _
  (render-below [_ entity* position]
    (shape-drawer/circle position 0.5 (color/rgb 1 1 1 0.6)))

  (destroy! [_ entity]
    (modifiers/reverse! (:parent @entity) stun-modifiers)))

(effects/defeffect :stun
  {:text (fn [{:keys [value]}]
           (str "Stuns for " (readable-number (/ value 1000)) " seconds"))
   :valid-params? (fn [{:keys [source target]}]
                    (and target)) ; TODO needs :speed/:skillmanager ?!
   :do! (fn [{:keys [target] duration :value}]
          (doseq-entity stun! target) ; TODO interrupt? (as sepearte ability also ? )
          (modifiers/apply! target stun-modifiers)
          (if-let [stun-effect (->> @target
                                    :children
                                    (filter #(::stun-effect @%))
                                    first)]
            (swap! stun-effect update-in [:delete-after-duration :maxcnt] + duration)
            (create-entity!
             {:position (:position @target)
              :parent target
              :z-order (:z-order @target)
              ::stun-effect true
              :delete-after-duration duration})))})
