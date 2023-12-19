(ns game.ui.skill-window
  (:require [gdl.context :refer [get-property]]
            [gdl.scene2d.ui :as ui]
            [app.state :refer [current-context]]
            [game.skill :as skill]
            [game.components.skills :as skills]))

; TODO this is actually two modifiers ... skill and free-skill-p
; and can do modifier-text @ tooltip ....
; 'skills' menu is a modifier menu ...
; you can have more general modifiers menus toggling on/off
; passives toggle-able ... wtf / stances ...
(defn- pressed-on-skill-in-menu [skill]
  (let [{:keys [context/player-entity]} @current-context]
    (when (and (pos? (:free-skill-points @player-entity))
               (not (skills/has-skill? (:skills @player-entity) skill)))
      (swap! player-entity #(-> %
                                (update :free-skill-points dec)
                                (update :skills skills/add-skill skill))))))

(defn create [context]
  (let [window (ui/window :title "Skills"
                          :id :skill-window)]
    (doseq [id [:projectile
                :meditation
                :spawn]
            :let [skill (get-property context id)
                  button (ui/image-button (:image skill)
                                          #(pressed-on-skill-in-menu skill))]]
      (.addListener button (ui/text-tooltip (fn []
                                              (let [context @current-context]
                                                (skill/text skill
                                                            (:context/player-entity context)
                                                            context)))))
      (.add window button))
    ; TODO render text label free-skill-points
    ; (str "Free points: " (:free-skill-points @player-entity))
    (.pack window)
    window))
