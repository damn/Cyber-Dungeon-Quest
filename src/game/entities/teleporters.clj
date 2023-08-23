(nsx game.entity.teleporters
  (:require [game.components.clickable :as clickable])
  (:use
    (game media)
    (game.maps
      [data :only (do-in-map current-map get-pretty-name)]
      mapchange)
    (game.components position body render)))

#_(defmethod clickable/on-clicked :teleporter [entity]
  (audio/play "bfxr_teleport.wav")
  (queue-map-change target-posi target-map save-game)
  (when do-after-use
    (do-after-use)))

#_(defnks create-teleporter
  [:position :target-map :target-posi :animation
   :opt :do-after-use :save-game]
  (db/create-entity!
   {:position position
    :body {:width 1.25
           :height 0.6
           :is-solid false}
    :render-on-minimap true
    :z-order :on-ground
    :animation animation
    :clickable {:type :teleporter
                :text (str "Teleport to " (get-pretty-name target-map))}}
   ;(light-component :intensity 0.8 :radius 2)
   ))

#_(defn static-teleporter
  [& {[start-map start-posi] :from
      [target-map target-posi] :to
      save-game :save-game}]
  (do-in-map start-map
    (create-teleporter
      :position start-posi
      :target-map target-map
      :target-posi target-posi
      :animation (create-animation (spritesheet-frames "teleporter/teleporter.png" 20 10) :frame-duration 100 :looping true)
      :save-game save-game)))

#_(defn connect-static-teleporters [& {start :from target :to}]
  (static-teleporter :from start :to target)
  (static-teleporter :from target :to start))

