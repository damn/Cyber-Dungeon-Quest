(ns game.context
  (:require gdl.app
            gdl.graphics.world
            gdl.graphics.gui
            [game.media :as media]))

(defn get-context [unit-scale]
  {:default-font media/font
   :unit-scale unit-scale
   :batch (:batch @gdl.app/state)
   :world/unit-scale gdl.graphics.world/unit-scale
   :gui/unit-scale gdl.graphics.gui/unit-scale})
