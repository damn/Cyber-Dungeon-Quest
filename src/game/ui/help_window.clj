(ns game.ui.help-window
  (:require [gdl.scene2d.ui :as ui]))

; TODO -> only open for first starting of the game or for every character only one time and save with save-game
; TODO -> hotkey
(def ^:private controls-text
  "* Moving: Leftmouse

  * Use skills: Left & right mouse

  * Set Skill hotkey: Press 0-9 while hovering
  over a skill at the bottom left selection lists.

  * Use items in actionbar: Q,W and E.

  * S   - skillmenu
  * C   - character info
  * I   - inventory
  * H   - help
  * ESC - exit/close menu
  * P   - Pause the game")

(defn create []
  (ui/window :id :help-window
             :title "Controls"
             :rows [[(ui/label controls-text)]]
             :pack? true))
