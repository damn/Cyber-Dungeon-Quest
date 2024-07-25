(ns cdq.context.ui.help-window
  (:require [gdl.context :refer [->window ->label]]))

(def ^:private controls-text
  "* Moving: WASD-keys
   * Use a skill: click leftmouse. Select in actionbar below.

  * S   - skillmenu
  * C   - character info
  * I   - inventory
  * H   - help
  * ESC - exit/close menu
  * P   - Pause the game")

(defn create [{{:keys [gui-viewport-width
                       gui-viewport-height]} :context/graphics
               :as context}]
  (->window context
            {:id :help-window
             :title "Controls"
             :visible? false
             :center-position [(/ gui-viewport-width 2)
                               gui-viewport-height]
             :rows [[(->label context controls-text)]]
             :pack? true}))
