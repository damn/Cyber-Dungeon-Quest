(ns game.context
  (:require gdl.app
            [game.media :as media]))

(defn get-context [unit-scale]
  (let [{:keys [batch world-unit-scale gui-unit-scale]} @gdl.app/state]
    {:default-font media/font
     :unit-scale unit-scale
     :batch batch
     :world-unit-scale world-unit-scale
     :gui-unit-scale gui-unit-scale}))
