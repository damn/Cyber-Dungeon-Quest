(ns game.context
  (:require gdl.graphics.unit-scale
            gdl.graphics.batch
            gdl.graphics.world
            gdl.graphics.gui
            [game.media :as media]))

(defn get-context []
  {:default-font media/font
   :unit-scale gdl.graphics.unit-scale/*unit-scale*
   :batch gdl.graphics.batch/batch
   :world/unit-scale gdl.graphics.world/unit-scale
   :gui/unit-scale gdl.graphics.gui/unit-scale})
