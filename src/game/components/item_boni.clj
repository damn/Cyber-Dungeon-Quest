(ns game.components.item-boni)

#_(defn- try-leech [dmg-dealt]
  (let [{:keys [hp-leech mana-leech]} (:item-boni @player-entity)]

    (when-not (zero? hp-leech)
      (let [value (* (/ hp-leech 100) dmg-dealt)]
        (show-gains-hp-effect player-entity value)
        (update-in! player-entity [:hp] inc-current value))
      )

    (when-not (zero? mana-leech)
      (let [value (* (/ mana-leech 100) dmg-dealt)]
        (show-gains-mana-effect player-entity value)
        (update-in! player-entity [:mana] inc-current value))
      )))

#_(def ^:private defaults
  {:hp-leech 0
   :mana-leech 0

   :chance-stun 0
   :chance-slow 0
   :chance-reduce-armor 0

   :dealt-melee-dmg-effect (fn [target-body]
                             #_(let [{:keys [chance-stun chance-slow chance-reduce-armor]} (:item-boni @player-entity)]
                               (when-chance chance-stun
                                 (stun target-body 1000))
                               (when-chance chance-slow
                                 (slowdown target-body 10))
                               (when-chance chance-reduce-armor
                                 (create-armor-reduce-effect target-body 50 10))))})
